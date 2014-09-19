package core.generator

import core.Util

case class ScalaClientMethodGenerator(
  config: ScalaClientMethodConfig,
  ssd: ScalaServiceDescription
) {
  import core.Text
  import core.Text._

  private val playUtil = Play2Util(config)

  private val sortedResources = ssd.resources.groupBy(_.model.plural).toSeq.sortBy(_._1)

  def traitsAndErrors(
    ssd: ScalaServiceDescription
  ): String = {
    (traits() + "\n\n" + failedRequestClass() + "\n\n" + errorPackage(ssd)).trim
  }

  def accessors(): String = {
    sortedResources.map { case (plural, resources) =>
      val methodName = Text.snakeToCamelCase(Text.camelCaseToUnderscore(plural).toLowerCase)
      s"def ${methodName}: ${plural} = ${plural}"
    }.mkString("\n\n")
  }

  def traits(): String = {
    sortedResources.map { case (plural, resources) =>
      s"trait $plural {\n" +
      methods(resources).map(_.interface).mkString("\n\n").indent(2) +
      "\n}"
    }.mkString("\n\n")
  }

  def objects(): String = {
    sortedResources.map { case (plural, resources) =>
      s"object $plural extends $plural {\n" +
      methods(resources).map(_.code).mkString("\n\n").indent(2) +
      "\n}"
    }.mkString("\n\n")
  }

  /**
    * Returns an implementation of FailedRequest class that is used to
    * capture errors in the client.
    */
  def failedRequestClass(): String = {
    Seq(
      s"case class FailedRequest(",
      s"  response: ${config.responseClass},",
      s"  message: Option[String] = None",
      s""") extends Exception(message.getOrElse(response.${config.responseStatusMethod} + ": " + response.${config.responseBodyMethod}))"""
    ).mkString("\n")
  }

  /**
    * Returns custom case classes based on the service description for
    * all errors return types. e.g. a 409 that returns Seq[Error] is
    * handled via these classes.
    */
  def errorPackage(ssd: ScalaServiceDescription): String = {
    ssd.resources.flatMap(_.operations).flatMap(_.responses).filter(r => !(r.isSuccess || r.isUnit)).map { response =>
      val etc = errorTypeClass(response).distinct.sorted.mkString("\n\n").indent(2)
      println(etc)
      Seq(
        "package error {",
        "",
        s"  import ${ssd.modelPackageName}.json._",
        "",
        errorTypeClass(response).indent(2),
        "}"
      ).mkString("\n")
    }.distinct.sorted.mkString("\n\n")
  }

  private[this] def errorTypeClass(response: ScalaResponse): String = {
    require(!response.isSuccess)

    val json = config.toJson("response", response.errorResponseType)

    // pass in status and UNPARSED body so that there is still a useful error
    // message even when the body is malformed and cannot be parsed
    Seq(
      s"case class ${response.errorClassName}(",
      s"  response: ${config.responseClass},",
      s"  message: Option[String] = None",
      s""") extends Exception(message.getOrElse(response.${config.responseStatusMethod} + ": " + response.${config.responseBodyMethod})) {""",
      s"  import ${ssd.modelPackageName}.json._",
      s"  lazy val ${response.errorVariableName} = ${json.indent(2).trim}",
      "}"
    ).mkString("\n")
  }

  private[this] def methods(resources: Seq[ScalaResource]): Seq[ClientMethod] = {

    resources.flatMap(_.operations).map { op =>
      val path = playUtil.pathParams(op)

      val payload = playUtil.formBody(op)
      val queryParameters = playUtil.params("queryParameters", op.queryParameters)

      val code = new scala.collection.mutable.ListBuffer[String]()
      val args = new scala.collection.mutable.ListBuffer[String]()
      payload.foreach { v =>
        code.append(v)
        args.append("body = Some(payload)")
      }

      queryParameters.foreach { v =>
        code.append(v)
        args.append("queryParameters = queryParameters")
      }

      val methodCall = code.toList match {
        case Nil => s"""_executeRequest("${op.method}", $path)"""
        case v => s"""${v.mkString("\n\n")}\n\n_executeRequest("${op.method}", $path, ${args.mkString(", ")})"""
      }

      val hasOptionResult = op.responses.filter(_.isSuccess).find(_.isOption) match {
        case None => ""
        case Some(r) => {
          if (r.isMultiple) {
            s"\ncase r if r.${config.responseStatusMethod} == 404 => Nil"
          } else {
            s"\ncase r if r.${config.responseStatusMethod} == 404 => None"
          }
        }
      }

      val matchResponse: String = {
        op.responses.flatMap { response =>
          if (response.isSuccess) {
            if (response.isOption) {
              if (response.isUnit) {
                Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => Some(Unit)")
              } else {
                val json = config.toJson("r", response.qualifiedScalaType)
                Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => Some($json)")
              }

            } else if (response.isMultiple) {
              val json = config.toJson("r", s"scala.collection.Seq[${response.qualifiedScalaType}]")
              Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => $json")

            } else if (response.isUnit) {
              Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => ${response.qualifiedScalaType}")

            } else {
              val json = config.toJson("r", response.qualifiedScalaType)
              Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => $json")
            }

          } else if (response.isNotFound && response.isOption) {
            // will be added later
            None

          } else {
            Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => throw new ${ssd.packageName}.error.${response.errorClassName}(r)")
          }
        }.mkString("\n")
      } + hasOptionResult + "\ncase r => throw new FailedRequest(r)\n"

      ClientMethod(
        name = op.name,
        argList = op.argList,
        returnType = s"scala.concurrent.Future[${op.resultType}]",
        methodCall = methodCall,
        response = matchResponse,
        comments = op.description
      )

    }
  }


  case class ClientMethod(
    name: String,
    argList: Option[String],
    returnType: String,
    methodCall: String,
    response: String,
    comments: Option[String]
  ) {
    import core.Text._
    
    private val commentString = comments.map(string => ScalaUtil.textToComment(string) + "\n").getOrElse("")

    val interface: String = {
      s"""${commentString}def $name(${argList.getOrElse("")})(implicit ec: scala.concurrent.ExecutionContext): $returnType"""
    }

    val code: String = {
      s"""override def $name(${argList.getOrElse("")})(implicit ec: scala.concurrent.ExecutionContext): $returnType = {
${methodCall.indent}.map {
${response.indent(4)}
  }
}"""
    }
  }

}
