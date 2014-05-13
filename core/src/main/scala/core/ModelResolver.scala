package core

/**
 * Recursively build models to make sure we can internally
 * resolve any references
 */
private[core] object ModelResolver {

  def build(internalModels: Seq[InternalModel], models: Seq[Model] = Seq.empty): Seq[Model] = {
    if (internalModels.isEmpty) {
      models
    } else {
      internalModels.find { im => referencesSatisfied(models, im) } match {
        case None => {
          sys.error("Circular dependencies found while trying to resolve references for models: " + internalModels.map(_.name).mkString(" "))
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

  def buildFields(models: Seq[Model], internalModel: InternalModel): Seq[Field] = {
    buildFields(models, internalModel, internalModel.fields, Seq.empty)
  }

  private def buildFields(models: Seq[Model], internalModel: InternalModel, internalFields: Seq[InternalField], fields: Seq[Field]): Seq[Field] = {
    if (internalFields.isEmpty) {
      fields
    } else {
      val field = Field(models, internalFields.head, Some(internalModel.plural), fields)
      val remainingFields = internalFields.drop(1)
      require(remainingFields.size == internalFields.size - 1)
      buildFields(models, internalModel, remainingFields, fields ++ Seq(field))
    }
  }

  private def referencesSatisfied(models: Seq[Model], im: InternalModel): Boolean = {
    im.fields.map { field =>
      field.fieldtype match {
        case None => true

        case Some(InternalNamedFieldType(name: String)) => {
          !Datatype.findByName(name).isEmpty || !models.find { _.name == name }.isEmpty
        }

        case Some(InternalReferenceFieldType(referencedModelName: String)) => {
          !models.find { _.name == referencedModelName }.isEmpty
        }

      }

    }.isEmpty
  }

}

