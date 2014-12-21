package generator

case class ScalaClientMethodGenerator(
  config: ScalaClientMethodConfig,
  ssd: ScalaService
) {
  import lib.Text
  import lib.Text._

  private val generatorUtil = GeneratorUtil(config)

  private val sortedResources = ssd.resources.groupBy(_.model.plural).toSeq.sortBy(_._1)

  def traitsAndErrors(): String = {
    (traits() + "\n\n" + failedRequestClass() + "\n\n" + errorPackage()).trim
  }

  def accessors(): String = {
    sortedResources.map { case (plural, resources) =>
      val methodName =lib.Text.snakeToCamelCase(lib.Text.camelCaseToUnderscore(plural).toLowerCase)
      config.accessor(methodName, plural)
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
    exceptionClass("FailedRequest")
  }

  /**
    * Returns custom case classes based on the service description for
    * all errors return types. e.g. a 409 that returns Seq[Error] is
    * handled via these classes.
    */
  def errorPackage(): String = {
    ssd.resources.flatMap(_.operations).flatMap(_.responses).filter(r => !(r.isSuccess || r.isUnit)).map { response =>
      val etc = errorTypeClass(response).distinct.sorted.mkString("\n\n").indent(2)
      val jsonImport = if (config.hasModelJsonPackage) {
        Seq("",
            s"  import ${ssd.modelPackageName}.json._",
            "")
      } else {
        Seq.empty
      }
      (Seq("package error {") ++
       jsonImport ++
       Seq(errorTypeClass(response).indent(2),
           "}")
      ).mkString("\n")
    }.distinct.sorted.mkString("\n\n")
  }

  private[this] def errorTypeClass(response: ScalaResponse): String = {
    require(!response.isSuccess)

    val json = config.toJson("response", response.datatype.name)
    val jsonImport = if (config.hasModelJsonPackage) Seq(s"import ${ssd.modelPackageName}.json._") else Seq.empty
    exceptionClass(response.errorClassName,
                   jsonImport :+ s"lazy val ${response.errorVariableName} = ${json.indent(2).trim}"
    )
  }

  private[this] def exceptionClass(
    className: String,
    body: Seq[String] = Seq.empty
  ): String = {
    val bodyString = body match {
      case Nil => ""
      case b => "{\n" + body.mkString("\n").indent(2) + "\n}"
    }

    Seq(
      s"case class $className(",
      s"  response: ${config.responseClass},",
      s"  message: Option[String] = None",
      s""") extends Exception(message.getOrElse(response.${config.responseStatusMethod} + ": " + response.${config.responseBodyMethod}))$bodyString"""
    ).mkString("\n")

  }

  private[this] def methods(resources: Seq[ScalaResource]): Seq[ClientMethod] = {

    resources.flatMap(_.operations).map { op =>
      val path = generatorUtil.pathParams(op)

      val payload = generatorUtil.formBody(op)
      val queryParameters = generatorUtil.params("queryParameters", op.queryParameters)

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

      val hasOptionResult = op.responses.filter(_.isSuccess).find(_.isOption).map { r =>
        s"\ncase r if r.${config.responseStatusMethod} == 404 => ${r.datatype.nilValue(r.`type`)}"
      }

      val matchResponse: String = {
        op.responses.flatMap { response =>
          if (response.isSuccess) {
            if (response.isOption) {
              if (response.isUnit) {
                Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => Some(Unit)")
              } else {
                val json = config.toJson("r", response.datatype.name)
                Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => Some($json)")
              }

            } else if (response.isUnit) {
              Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => ${response.datatype.name}")

            } else {
              val json = config.toJson("r", response.datatype.name)
              Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => $json")
            }

          } else if (response.isNotFound && response.isOption) {
            // will be added later
            None

          } else {
            Some(s"case r if r.${config.responseStatusMethod} == ${response.code} => throw new ${ssd.packageName}.error.${response.errorClassName}(r)")
          }
        }.mkString("\n")
      } + hasOptionResult.getOrElse("") + "\ncase r => throw new FailedRequest(r)\n"

      ClientMethod(
        name = op.name,
        argList = op.argList,
        returnType = hasOptionResult match {
          case None => s"scala.concurrent.Future[${op.resultType}]"
          case Some(_) => s"scala.concurrent.Future[scala.Option[${op.resultType}]]"
        },
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
    import lib.Text._
    
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
