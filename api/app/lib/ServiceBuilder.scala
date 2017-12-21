package lib

import io.apibuilder.spec.v0.models.{Enum, EnumValue, Field, Model, Service, Union, UnionType}

object ServiceBuilder {
  implicit class ServiceBuilder(val service: Service) extends AnyVal {

    def withModel(name: String,
                  modelTaylor: Model => Model = identity): Service = {
      val model = Model(
        name = name,
        plural = s"${name}s",
        fields = Nil
      )

      service.copy(
        models = service.models ++ Seq(modelTaylor(model))
      )
    }

    def withEnum(name: String,
                 enumTailor: Enum => Enum = identity): Service = {
      val enum = Enum(
        name = name,
        plural = s"${name}s",
        values = Nil
      )

      service.copy(
        enums = service.enums ++ Seq(enumTailor(enum))
      )
    }

    def withUnion(name: String,
                  unionTailor: Union => Union = identity,
                  discriminator: Option[String] = None): Service = {
      val union = Union(
        name = name,
        plural = s"${name}s",
        discriminator = discriminator,
        types = Nil
      )
      service.copy(
        unions = service.unions ++ Seq(unionTailor(union))
      )
    }
  }

  implicit class ModelBuilder(val model: Model) extends AnyVal {
    def withField(name: String,
                  fieldType: String,
                  default: Option[String] = None,
                  required: Boolean = true): Model = {
      val field = Field(
        name = name,
        `type` = fieldType,
        default = default,
        required = required)
      model.copy(fields = model.fields ++ Seq(field))
    }

  }

  implicit class EnumBuilder(val enum: Enum) extends AnyVal {
    def withValue(name: String,
                  value: Option[String] = None): Enum = {
      val enumValue = EnumValue(
        name = name,
        value = value
      )
      enum.copy(
        values = enum.values ++ Seq(enumValue)
      )
    }

  }

  implicit class UnionBuilder(val union: Union) extends AnyVal {
    def withType(`type`: String,
                 discriminatorValue: Option[String] = None,
                 isDefault: Boolean = false): Union = {
      val unionType = UnionType(
        `type` = `type`,
        default = Some(true).filter(_ => isDefault),
        discriminatorValue = discriminatorValue
      )
      union.copy(
        types = union.types ++ Seq(unionType)
      )
    }
  }
}
