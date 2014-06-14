package apidoc.models {
  case class User(
    guid: java.util.UUID,
    email: String,
    name: String,
    imageUrl: scala.Option[String] = None
  )
  case class Organization(
    guid: java.util.UUID,
    key: String,
    name: String
  )
  case class Membership(
    guid: java.util.UUID,
    user: User,
    organization: Organization,
    role: String
  )
  case class MembershipRequest(
    guid: java.util.UUID,
    user: User,
    organization: Organization,
    role: String
  )
  case class Service(
    guid: java.util.UUID,
    name: String,
    key: String,
    description: scala.Option[String] = None
  )
  case class Version(
    guid: java.util.UUID,
    version: String,
    json: String
  )
  case class Code(
    version: Version,
    target: String,
    source: String
  )
  case class CodeError(
    code: String,
    message: String,
    validTargets: scala.collection.Seq[String] = Nil
  )
  case class Error(
    code: String,
    message: String
  )
}

package apidoc.models {
  package object json {
    import play.api.libs.json._
    import play.api.libs.functional.syntax._

    implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    implicit val readsUser: play.api.libs.json.Reads[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "email").read[String] and
         (__ \ "name").read[String] and
         (__ \ "image_url").readNullable[String])(User.apply _)
      }
    
    implicit val writesUser: play.api.libs.json.Writes[User] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "email").write[String] and
         (__ \ "name").write[String] and
         (__ \ "image_url").write[scala.Option[String]])(unlift(User.unapply))
      }
    
    implicit val readsOrganization: play.api.libs.json.Reads[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "key").read[String] and
         (__ \ "name").read[String])(Organization.apply _)
      }
    
    implicit val writesOrganization: play.api.libs.json.Writes[Organization] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "key").write[String] and
         (__ \ "name").write[String])(unlift(Organization.unapply))
      }
    
    implicit val readsMembership: play.api.libs.json.Reads[Membership] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "user").read[User] and
         (__ \ "organization").read[Organization] and
         (__ \ "role").read[String])(Membership.apply _)
      }
    
    implicit val writesMembership: play.api.libs.json.Writes[Membership] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "user").write[User] and
         (__ \ "organization").write[Organization] and
         (__ \ "role").write[String])(unlift(Membership.unapply))
      }
    
    implicit val readsMembershipRequest: play.api.libs.json.Reads[MembershipRequest] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "user").read[User] and
         (__ \ "organization").read[Organization] and
         (__ \ "role").read[String])(MembershipRequest.apply _)
      }
    
    implicit val writesMembershipRequest: play.api.libs.json.Writes[MembershipRequest] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "user").write[User] and
         (__ \ "organization").write[Organization] and
         (__ \ "role").write[String])(unlift(MembershipRequest.unapply))
      }
    
    implicit val readsService: play.api.libs.json.Reads[Service] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "name").read[String] and
         (__ \ "key").read[String] and
         (__ \ "description").readNullable[String])(Service.apply _)
      }
    
    implicit val writesService: play.api.libs.json.Writes[Service] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "name").write[String] and
         (__ \ "key").write[String] and
         (__ \ "description").write[scala.Option[String]])(unlift(Service.unapply))
      }
    
    implicit val readsVersion: play.api.libs.json.Reads[Version] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").read[java.util.UUID] and
         (__ \ "version").read[String] and
         (__ \ "json").read[String])(Version.apply _)
      }
    
    implicit val writesVersion: play.api.libs.json.Writes[Version] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "guid").write[java.util.UUID] and
         (__ \ "version").write[String] and
         (__ \ "json").write[String])(unlift(Version.unapply))
      }
    
    implicit val readsCode: play.api.libs.json.Reads[Code] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "version").read[Version] and
         (__ \ "target").read[String] and
         (__ \ "source").read[String])(Code.apply _)
      }
    
    implicit val writesCode: play.api.libs.json.Writes[Code] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "version").write[Version] and
         (__ \ "target").write[String] and
         (__ \ "source").write[String])(unlift(Code.unapply))
      }
    
    implicit val readsCodeError: play.api.libs.json.Reads[CodeError] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "code").read[String] and
         (__ \ "message").read[String] and
         (__ \ "valid_targets").readNullable[scala.collection.Seq[String]].map { x =>
          x.getOrElse(Nil)
        })(CodeError.apply _)
      }
    
    implicit val writesCodeError: play.api.libs.json.Writes[CodeError] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "code").write[String] and
         (__ \ "message").write[String] and
         (__ \ "valid_targets").write[scala.collection.Seq[String]])(unlift(CodeError.unapply))
      }
    
    implicit val readsError: play.api.libs.json.Reads[Error] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "code").read[String] and
         (__ \ "message").read[String])(Error.apply _)
      }
    
    implicit val writesError: play.api.libs.json.Writes[Error] =
      {
        import play.api.libs.json._
        import play.api.libs.functional.syntax._
        ((__ \ "code").write[String] and
         (__ \ "message").write[String])(unlift(Error.unapply))
      }
  }
}
