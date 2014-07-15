package core

import play.api.libs.json.JsValue
import com.fasterxml.jackson.core.{ JsonParseException, JsonProcessingException }
import com.fasterxml.jackson.databind.JsonMappingException

case class ServiceDescriptionValidator(apiJson: String) {

  private val RequiredFields = Seq("base_url", "name")

  private var parseError: Option[String] = None

  lazy val serviceDescription: Option[ServiceDescription] = {
    internalServiceDescription.map { ServiceDescription(_) }
  }

  lazy val internalServiceDescription: Option[InternalServiceDescription] = {
    try {
      Some(InternalServiceDescription(apiJson))
    } catch {
      case e: JsonParseException => {
        parseError = Some(e.getMessage)
        None
      }
      case e: JsonProcessingException => {
        parseError = Some(e.getMessage)
        None
      }
      case e: JsonMappingException => {
        parseError = Some(e.getMessage)
        None
      }
      case e: Throwable => throw e
    }
  }

  lazy val errors: Seq[String] = {
    internalServiceDescription match {

      case None => {
        if (apiJson == "") {
          Seq("No Data")
        } else {
          Seq(parseError.getOrElse("Invalid JSON"))
        }
      }

      case Some(sd: InternalServiceDescription) => {
        val requiredFieldErrors = validateRequiredFields()

        if (requiredFieldErrors.isEmpty) {
          validateName ++
          validateModels ++
          validateFields ++
          validateParameterTypes ++
          validateFieldTypes ++
          validateFieldDefaults ++
          validateResources ++
          validateParameters ++
          validateResponses ++
          validatePathParameters ++
          validatePathParametersAreRequired
        } else {
          requiredFieldErrors
        }
      }
    }
  }

  def isValid: Boolean = {
    errors.isEmpty
  }


  private def validateName(): Seq[String] = {
    if (Text.startsWithLetter(serviceDescription.get.name)) {
      Seq.empty
    } else {
      Seq(s"Name[${serviceDescription.get.name}] must start with a letter")
    }
  }

  /**
   * Validate basic structure, returning a list of error messages
   */
  private def validateRequiredFields(): Seq[String] = {
    val missing = RequiredFields.filter { field =>
      (internalServiceDescription.get.json \ field).asOpt[JsValue] match {
        case None => true
        case Some(_) => false
      }
    }
    if (missing.isEmpty) {
      Seq.empty
    } else {
      Seq("Missing: " + missing.mkString(", "))
    }
  }

  /**
   * Validate references to ensure they refer to proper data
   */
  private def validateFieldTypes(): Seq[String] = {
    internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter { f => !f.fieldtype.isEmpty && !f.name.isEmpty }.flatMap { field =>
        val name = field.fieldtype.get
        Datatype.findByName(name) match {
          case None => {
            internalServiceDescription.get.models.find { _.name == name } match {
              case None => Some(s"${model.name}.${field.name.get} has invalid type. There is no model nor datatype named[$name]")
              case Some(_) => None
            }
          }
          case Some(_) => None
        }
      }
    }
  }

  /**
   * Validates that any defaults specified for fields are valid for the field datatype
   */
  private def validateFieldDefaults(): Seq[String] = {
    internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter { f => !f.fieldtype.isEmpty && !f.name.isEmpty && !f.default.isEmpty }.flatMap { field =>
        val name = field.fieldtype.get
        Datatype.findByName(name).flatMap { dt =>
          if (Field.isValid(dt, field.default.get)) {
            None
          } else {
            Some(s"Model[${model.name}] field[${field.name.get}] Default[${field.default.get}] is not valid for datatype[$name]")
          }
        }
      }
    }
  }

  private def validateModels(): Seq[String] = {
    val nameErrors = internalServiceDescription.get.models.flatMap { model =>
      val errors = Text.validateName(model.name)
      if (errors.isEmpty) {
        None
      } else {
        Some(s"Model[${model.name}] name is invalid: ${errors.mkString(" ")}")
      }
    }

    val fieldErrors = internalServiceDescription.get.models.filter { _.fields.isEmpty }.map { model =>
      s"Model[${model.name}] must have at least one field"
    }

    val duplicates = internalServiceDescription.get.models.groupBy(_.name).filter { _._2.size > 1 }.keys.map { modelName =>
      s"Model[$modelName] appears more than once"
    }

    nameErrors ++ fieldErrors ++ duplicates
  }

  private def validateFields(): Seq[String] = {
    val missingTypes = internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter { _.fieldtype.isEmpty }.map { f =>
        s"Model[${model.name}] field[${f.name.get}] must have a type"
      }
    }
    val missingNames = internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter { _.name.isEmpty }.map { f =>
        s"Model[${model.name}] field[${f.name}] must have a name"
      }
    }
    val badNames = internalServiceDescription.get.models.flatMap { model =>
      model.fields.flatMap { f =>
        f.name.map { n => n -> Text.validateName(n) }
      }.filter(_._2.nonEmpty).flatMap { case (name, errors) =>
          errors.map { e =>
            s"Model[${model.name}] field[${name}]: $e"
          }
      }
    }

    val badValues = internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter(!_.enum.isEmpty).flatMap { f =>
        f.enum.map { n => n -> Text.validateName(n) }
      }.filter(_._2.nonEmpty).flatMap { case (name, errors) =>
          errors.map { e =>
            s"Invalid value for Model[${model.name}] field[${name}]: $e"
          }
      }
    }

    /**
      * Currently we only support enumeration values for string
      * datatypes. This may change in future but was the simplest and
      * most common use case to fully support.
      */
    val enumsForNonStringTypes = internalServiceDescription.get.models.flatMap { model =>
      model.fields.filter(!_.name.isEmpty).filter(!_.enum.isEmpty).filter(_.fieldtype != Some("string")).map { f =>
        s"Model[${model.name}] field[${f.name.get}]: enum can only be specified for fields of type 'string'"
      }
    }

    missingTypes ++ missingNames ++ badNames ++ badValues ++ enumsForNonStringTypes
  }

  private def validateResponses(): Seq[String] = {
    val invalidCodes = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.flatMap { r =>
          try {
            r.code.toInt
            None
          } catch {
            case e: java.lang.NumberFormatException => {
              Some(s"Resource[${resource.modelName.getOrElse("")}] ${op.label}: Response code is not an integer[${r.code}]")
            }
          }
        }
      }
    }

    val modelNames = internalServiceDescription.get.models.map { _.name }.toSet

    val missingTypes = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.responses.flatMap { r =>
          r.datatype match {
            case None => {
              Some(s"Resource[${resource.modelName.getOrElse("")}] ${op.label} with response code[${r.code}]: Missing type")
            }
            case Some(typeName: String) => {
              Datatype.findByName(typeName) match {
                case Some(dt: Datatype) => {
                  None
                }
                case None => {
                  if (modelNames.contains(typeName)) {
                    None
                  } else {
                    Some(s"Resource[${resource.modelName.getOrElse("")}] ${op.label} with response code[${r.code}] has an invalid type[${typeName}]. Must be one of: ${ValidDatatypes.mkString(" ")} or the name of a model")
                  }
                }
              }
            }
          }
        }
      }
    }

    val mixed2xxResponseTypes = if (invalidCodes.isEmpty) {
      internalServiceDescription.get.resources.filter { !_.modelName.isEmpty }.flatMap { resource =>
        resource.operations.flatMap { op =>
          val types = op.responses.filter { r => !r.datatypeLabel.isEmpty && r.code.toInt >= 200 && r.code.toInt < 300 }.map(_.datatypeLabel.get).distinct
          if (types.size <= 1) {
            None
          } else {
            Some(s"Resource[${resource.modelName.get}] cannot have varying response types for 2xx response codes: ${types.sorted.mkString(", ")}")
          }
        }
      }
    } else {
      Seq.empty
    }

    val typesRequiringUnit = Seq(204, 304, 404)
    val noContentWithTypes = if (invalidCodes.isEmpty) {
      internalServiceDescription.get.resources.filter { !_.modelName.isEmpty }.flatMap { resource =>
        resource.operations.flatMap { op =>
          op.responses.filter(r => typesRequiringUnit.contains(r.code.toInt) && !r.datatype.isEmpty && r.datatype.get != Datatype.UnitType.name).map { r =>
            s"Resource[${resource.modelName.get}] ${op.label} Responses w/ code[${r.code}] must return unit and not[${r.datatype.get}]"
          }
        }
      }
    } else {
      Seq.empty
    }

    invalidCodes ++ missingTypes ++ mixed2xxResponseTypes ++ noContentWithTypes
  }

  private lazy val ValidDatatypes = Datatype.All.map(_.name).sorted
  private lazy val ValidQueryDatatypes = ValidDatatypes
  //private lazy val ValidQueryDatatypes = Datatype.QueryParameterTypes.map(_.name).sorted

  private def validateParameters(): Seq[String] = {
    val missingNames = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.parameters.filter { p => p.name.isEmpty }.map { p =>
          s"Resource[${resource.modelName.getOrElse("")}] ${op.method.get} ${op.path}: All parameters must have a name"
        }
      }
    }

    val missingTypes = internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.filter(!_.method.isEmpty).flatMap { op =>
        val types = if (Util.isJsonDocumentMethod(op.method.get)) { ValidDatatypes } else { ValidQueryDatatypes }

        op.parameters.filter { !_.name.isEmpty }.flatMap { p =>
          if (p.paramtype.isEmpty) {
            Some(s"Resource[${resource.modelName.getOrElse("")}] ${op.method.get} ${op.path}: Parameter[${p.name.get}] is missing a type. Must be one of: ${types.mkString(" ")}")
          } else if (!types.contains(p.paramtype.get)) {
            Some(s"Resource[${resource.modelName.getOrElse("")}] ${op.method.get} ${op.path}: Parameter[${p.name.get}] has an invalid type[${p.paramtype.get}]. Must be one of: ${types.mkString(" ")}")
          } else {
            None
          }
        }
      }
    }

    missingNames ++ missingTypes
  }

  private def validateParameterTypes(): Seq[String] = {
    internalServiceDescription.get.resources.flatMap { resource =>
      resource.operations.flatMap { op =>
        op.parameters.filter( !_.paramtype.isEmpty ).flatMap { param =>

          val typeName = param.paramtype.get

          Datatype.findByName(typeName) match {

            case Some(dt: Datatype) => {
              None
            }

            case None => {
              internalServiceDescription.get.models.find(_.name == typeName) match {
                case None => {
                  Some(s"Resource[${resource.modelName.getOrElse("")}] ${op.method.get} ${op.path}: Parameter[${param.name.get}] has an invalid datatype[${typeName}]. Must be one of: ${ValidDatatypes.mkString(" ")} or the name of a model")
                }
                case Some(m: InternalModel) => {
                  None
                }
              }
            }
          }
        }
      }
    }
  }

  private def validateResources(): Seq[String] = {
    val modelNameErrors = internalServiceDescription.get.resources.flatMap { res =>
      res.modelName match {
        case None => Some("All resources must have a model")
        case Some(name: String) => {
          internalServiceDescription.get.models.find { _.name == name } match {
            case None => Some(s"Resource[${res.modelName.getOrElse("")}] model name[${name}] is invalid - model not found")
            case Some(_) => None
          }
        }
      }
    }

    val missingOperations = internalServiceDescription.get.resources.filter { _.operations.isEmpty }.map { res =>
      s"Resource[${res.modelName.getOrElse("")}] must have at least one operation"
    }

    val duplicateModels = internalServiceDescription.get.resources.filter { !_.modelName.isEmpty }.flatMap { r =>
      val numberResources = internalServiceDescription.get.resources.filter { _.modelName == r.modelName }.size
      if (numberResources <= 1) {
        None
      } else {
        Some(s"Model[${r.modelName.get}] cannot be mapped to more than one resource")
      }
    }.distinct

    modelNameErrors ++ missingOperations ++ duplicateModels
  }

  private def validatePathParameters(): Seq[String] = {
    internalServiceDescription.get.resources.filter(!_.modelName.isEmpty).flatMap { resource =>
      internalServiceDescription.get.models.find(_.name == resource.modelName.get) match {
        case None => None
        case Some(model: InternalModel) => {
          resource.operations.filter(!_.namedPathParameters.isEmpty).flatMap { op =>
            val fieldMap = model.fields.filter(f => !f.name.isEmpty && !f.fieldtype.isEmpty).map(f => (f.name.get -> f.fieldtype.get)).toMap
            val paramMap = op.parameters.filter(p => !p.name.isEmpty && !p.paramtype.isEmpty).map(p => (p.name.get -> p.paramtype.get)).toMap

            op.namedPathParameters.flatMap { name =>
              val typeName = paramMap.get(name).getOrElse {
                fieldMap.get(name).getOrElse {
                  Datatype.StringType.name
                }
              }

              Datatype.findByName(typeName) match {
                case Some(Datatype.BooleanType) => None
                case Some(Datatype.DecimalType) => None
                case Some(Datatype.IntegerType) => None
                case Some(Datatype.DoubleType) => None
                case Some(Datatype.LongType) => None
                case Some(Datatype.StringType) => None
                case Some(Datatype.UuidType) => None
                case _ => {
                  Some(s"Resource[${resource.modelName.get}] ${op.method.getOrElse("")} path parameter[$name] has an invalid type[$typeName]. Only numbers and strings can be specified as path parameters")
                }
              }
            }
          }
        }
      }
    }
  }

  private def validatePathParametersAreRequired(): Seq[String] = {
    internalServiceDescription.get.resources.filter(!_.modelName.isEmpty).flatMap { resource =>
      internalServiceDescription.get.models.find(_.name == resource.modelName.get) match {
        case None => None
        case Some(model: InternalModel) => {
          resource.operations.filter(!_.namedPathParameters.isEmpty).flatMap { op =>
            val fieldMap = model.fields.filter(f => !f.name.isEmpty && !f.fieldtype.isEmpty).map(f => (f.name.get -> f.required)).toMap
            val paramMap = op.parameters.filter(p => !p.name.isEmpty && !p.paramtype.isEmpty).map(p => (p.name.get -> p.required)).toMap

            op.namedPathParameters.flatMap { name =>
              val isRequired = paramMap.get(name).getOrElse {
                fieldMap.get(name).getOrElse {
                  true
                }
              }

              if (isRequired) {
                None
              } else {
                Some(s"Resource[${resource.modelName.get}] ${op.method.getOrElse("")} path parameter[$name] is specified as optional. All path parameters are required")
              }
            }
          }
        }
      }
    }
  }
}
