package builder.api_json

case class InterfaceInheritance(interfaces: Seq[InternalInterfaceForm]) {

  private[this] lazy val byName: Map[String, InternalInterfaceForm] = interfaces.map { i => i.name -> i }.toMap

  /**
   * For each model:
   *   - iterate through all interfaces
   *   - if the interface defines a field that is not already specified on
   *     the model, append it
   * The returned list of models will contain ALL fields specified across
   * all interfaces.
   */
  def models(forms: Seq[InternalModelForm]): Seq[InternalModelForm] = {
    forms.map { f =>
      f.copy(
        fields = withInheritedFields(f)
      )
    }
  }

  private[this] def withInheritedFields(form: InternalModelForm): Seq[InternalFieldForm] = {
    form.interfaces.foldLeft(form.fields) { case (all, interfaceName) =>
      all ++ fields(interfaceName, all.flatMap(_.name))
    }
  }

  private[this] def fields(interfaceName: String, definedFields: Seq[String]): Seq[InternalFieldForm] = {
    byName.get(interfaceName) match {
      case None => Nil
      case Some(interface) => {
        interface.fields.filter { f =>
          f.name match {
            case None => false
            case Some(n) => !definedFields.contains(n)
          }
        }
      }
    }
  }
}
