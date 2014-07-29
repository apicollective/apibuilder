package core

/**
 * Recursively build models to make sure we can internally
 * resolve any references
 */
private[core] object ModelResolver {

  case class CircularReferenceException(message: String) extends Exception(message)

  def build(internalModels: Seq[InternalModel], models: Seq[Model] = Seq.empty): Seq[Model] = {
    if (internalModels.isEmpty) {
      models
    } else {
      internalModels.find { im => referencesSatisfied(im, models) } match {
        case None => {
          throw new CircularReferenceException("Circular dependencies found while trying to resolve references for models: " + internalModels.map(_.name).mkString(" "))
        }

        case Some(nextModel: InternalModel) => {
          val fullModel = Model(models, nextModel)
          val remainingModels = internalModels.filter { _ != nextModel }
          require(remainingModels.size == internalModels.size - 1)
          build(remainingModels, models ++ Seq(fullModel))
        }
      }
    }
  }

  def buildFields(internalModel: InternalModel, models: Seq[Model]): Seq[Field] = {
    buildFields(internalModel, models, internalModel.fields, Seq.empty)
  }

  private def buildFields(internalModel: InternalModel, models: Seq[Model], internalFields: Seq[InternalField], fields: Seq[Field]): Seq[Field] = {
    if (internalFields.isEmpty) {
      fields
    } else {
      val field = Field(models, internalModel, internalFields.head)
      val remainingFields = internalFields.drop(1)
      require(remainingFields.size == internalFields.size - 1)
      buildFields(internalModel, models, remainingFields, fields ++ Seq(field))
    }
  }

  private def referencesSatisfied(im: InternalModel, models: Seq[Model]): Boolean = {
    im.fields.find { field =>
      field.fieldtype match {
        case None => false

        case Some(name: String) => {
          Datatype.findByName(name).isEmpty && models.find { _.name == name }.isEmpty
        }
      }
    }.isEmpty
  }

}

