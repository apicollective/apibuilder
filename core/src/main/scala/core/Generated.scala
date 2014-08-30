package com.gilt.apidoc.models {
  /**
   * Generated source code.
   */
  case class Code(
    target: Target,
    source: String
  )

  /**
   * Represents a single domain name (e.g. www.apidoc.me). When a new user registers
   * and confirms their email, we automatically associate that user with a member of
   * the organization associated with their domain. For example, if you confirm your
   * account with an email address of foo@gilt.com, we will automatically add you as
   * a member to the organization with domain gilt.com.
   */
  case class Domain(
    name: String
  )

  case class Error(
    code: String,
    message: String
  )

  case class Healthcheck(
    status: String
  )

  /**
   * A membership represents a user in a specific role to an organization.
   * Memberships cannot be created directly. Instead you first create a membership
   * request, then that request is either accepted or declined.
   */
  case class Membership(
    guid: java.util.UUID,
    user: User,
    organization: Organization,
    role: String
  )

  /**
   * A membership request represents a user requesting to join an organization with a
   * specificed role (e.g. as a member or an admin). Membership requests can be
   * reviewed by any current admin of the organization who can either accept or
   * decline the request.
   */
  case class MembershipRequest(
    guid: java.util.UUID,
    user: User,
    organization: Organization,
    role: String
  )

  /**
   * An organization is used to group a set of services together.
   */
  case class Organization(
    guid: java.util.UUID,
    key: String,
    name: String,
    domains: scala.collection.Seq[Domain] = Nil,
    metadata: scala.Option[OrganizationMetadata] = None
  )

  /**
   * Supplemental (non-required) information about an organization
   */
  case class OrganizationMetadata(
    packageName: scala.Option[String] = None
  )

  /**
   * A service has a name and multiple versions of an API (Interface).
   */
  case class Service(
    guid: java.util.UUID,
    name: String,
    key: String,
    visibility: Visibility,
    description: scala.Option[String] = None
  )

  /**
   * A user is a top level person interacting with the api doc server.
   */
  case class User(
    guid: java.util.UUID,
    email: String,
    name: scala.Option[String] = None
  )

  /**
   * Used only to validate json files - used as a resource where http status code
   * defines success
   */
  case class Validation(
    valid: Boolean,
    errors: scala.collection.Seq[String] = Nil
  )

  /**
   * Represents a unique version of the service.
   */
  case class Version(
    guid: java.util.UUID,
    version: String,
    json: String
  )

  /**
   * The target platform for code generation.
   */
  sealed trait Target

  object Target {

    /**
     * Generates an avro JSON schema
     */
    case object AvroSchema extends Target { override def toString = "avro_schema" }
    /**
     * Generates a client w/ no dependencies external to play framework 2.2
     */
    case object Play22Client extends Target { override def toString = "play_2_2_client" }
    /**
     * Generates a client w/ no dependencies external to play framework 2.3
     */
    case object Play23Client extends Target { override def toString = "play_2_3_client" }
    case object Play2XJson extends Target { override def toString = "play_2_x_json" }
    case object Play2XRoutes extends Target { override def toString = "play_2_x_routes" }
    case object RubyClient extends Target { override def toString = "ruby_client" }
    case object ScalaModels extends Target { override def toString = "scala_models" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Target

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(AvroSchema, Play22Client, Play23Client, Play2XJson, Play2XRoutes, RubyClient, ScalaModels)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): Target = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[Target] = byName.get(value)

  }

  /**
   * Controls who is able to view this version
   */
  sealed trait Visibility

  object Visibility {

    /**
     * Any member of the organization can view this service
     */
    case object Organization extends Visibility { override def toString = "organization" }
    /**
     * Anybody, including non logged in users, can view this service
     */
    case object Public extends Visibility { override def toString = "public" }

    /**
     * UNDEFINED captures values that are sent either in error or
     * that were added by the server after this library was
     * generated. We want to make it easy and obvious for users of
     * this library to handle this case gracefully.
     *
     * We use all CAPS for the variable name to avoid collisions
     * with the camel cased values above.
     */
    case class UNDEFINED(override val toString: String) extends Visibility

    /**
     * all returns a list of all the valid, known values. We use
     * lower case to avoid collisions with the camel cased values
     * above.
     */
    val all = Seq(Organization, Public)

    private[this]
    val byName = all.map(x => x.toString -> x).toMap

    def apply(value: String): Visibility = fromString(value).getOrElse(UNDEFINED(value))

    def fromString(value: String): scala.Option[Visibility] = byName.get(value)

  }
}

package com.gilt.apidoc.models {
  package object json {
    import play.api.libs.json.__
    import play.api.libs.json.JsString
    import play.api.libs.json.Writes
    import play.api.libs.functional.syntax._

    private[apidoc] implicit val jsonReadsUUID = __.read[String].map(java.util.UUID.fromString)

    private[apidoc] implicit val jsonWritesUUID = new Writes[java.util.UUID] {
      def writes(x: java.util.UUID) = JsString(x.toString)
    }

    private[apidoc] implicit val jsonReadsJodaDateTime = __.read[String].map { str =>
      import org.joda.time.format.ISODateTimeFormat.dateTimeParser
      dateTimeParser.parseDateTime(str)
    }

    private[apidoc] implicit val jsonWritesJodaDateTime = new Writes[org.joda.time.DateTime] {
      def writes(x: org.joda.time.DateTime) = {
        import org.joda.time.format.ISODateTimeFormat.dateTime
        val str = dateTime.print(x)
        JsString(str)
      }
    }

    implicit val jsonReadsApiDocEnum_Target = __.read[String].map(Target.apply)
    implicit val jsonWritesApiDocEnum_Target = new Writes[Target] {
      def writes(x: Target) = JsString(x.toString)
    }

    implicit val jsonReadsApiDocEnum_Visibility = __.read[String].map(Visibility.apply)
    implicit val jsonWritesApiDocEnum_Visibility = new Writes[Visibility] {
      def writes(x: Visibility) = JsString(x.toString)
    }
    implicit def jsonReadsApiDocCode: play.api.libs.json.Reads[Code] = {
      (
        (__ \ "target").read[Target] and
        (__ \ "source").read[String]
      )(Code.apply _)
    }

    implicit def jsonWritesApiDocCode: play.api.libs.json.Writes[Code] = {
      (
        (__ \ "target").write[Target] and
        (__ \ "source").write[String]
      )(unlift(Code.unapply _))
    }

    implicit def jsonReadsApiDocDomain: play.api.libs.json.Reads[Domain] = {
      (__ \ "name").read[String].map { x => new Domain(name = x) }
    }

    implicit def jsonWritesApiDocDomain: play.api.libs.json.Writes[Domain] = new play.api.libs.json.Writes[Domain] {
      def writes(x: Domain) = play.api.libs.json.Json.obj(
        "name" -> play.api.libs.json.Json.toJson(x.name)
      )
    }

    implicit def jsonReadsApiDocError: play.api.libs.json.Reads[Error] = {
      (
        (__ \ "code").read[String] and
        (__ \ "message").read[String]
      )(Error.apply _)
    }

    implicit def jsonWritesApiDocError: play.api.libs.json.Writes[Error] = {
      (
        (__ \ "code").write[String] and
        (__ \ "message").write[String]
      )(unlift(Error.unapply _))
    }

    implicit def jsonReadsApiDocHealthcheck: play.api.libs.json.Reads[Healthcheck] = {
      (__ \ "status").read[String].map { x => new Healthcheck(status = x) }
    }

    implicit def jsonWritesApiDocHealthcheck: play.api.libs.json.Writes[Healthcheck] = new play.api.libs.json.Writes[Healthcheck] {
      def writes(x: Healthcheck) = play.api.libs.json.Json.obj(
        "status" -> play.api.libs.json.Json.toJson(x.status)
      )
    }

    implicit def jsonReadsApiDocMembership: play.api.libs.json.Reads[Membership] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "user").read[User] and
        (__ \ "organization").read[Organization] and
        (__ \ "role").read[String]
      )(Membership.apply _)
    }

    implicit def jsonWritesApiDocMembership: play.api.libs.json.Writes[Membership] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "user").write[User] and
        (__ \ "organization").write[Organization] and
        (__ \ "role").write[String]
      )(unlift(Membership.unapply _))
    }

    implicit def jsonReadsApiDocMembershipRequest: play.api.libs.json.Reads[MembershipRequest] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "user").read[User] and
        (__ \ "organization").read[Organization] and
        (__ \ "role").read[String]
      )(MembershipRequest.apply _)
    }

    implicit def jsonWritesApiDocMembershipRequest: play.api.libs.json.Writes[MembershipRequest] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "user").write[User] and
        (__ \ "organization").write[Organization] and
        (__ \ "role").write[String]
      )(unlift(MembershipRequest.unapply _))
    }

    implicit def jsonReadsApiDocOrganization: play.api.libs.json.Reads[Organization] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "key").read[String] and
        (__ \ "name").read[String] and
        (__ \ "domains").readNullable[scala.collection.Seq[Domain]].map(_.getOrElse(Nil)) and
        (__ \ "metadata").readNullable[OrganizationMetadata]
      )(Organization.apply _)
    }

    implicit def jsonWritesApiDocOrganization: play.api.libs.json.Writes[Organization] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "key").write[String] and
        (__ \ "name").write[String] and
        (__ \ "domains").write[scala.collection.Seq[Domain]] and
        (__ \ "metadata").write[scala.Option[OrganizationMetadata]]
      )(unlift(Organization.unapply _))
    }

    implicit def jsonReadsApiDocOrganizationMetadata: play.api.libs.json.Reads[OrganizationMetadata] = {
      (__ \ "package_name").readNullable[String].map { x => new OrganizationMetadata(packageName = x) }
    }

    implicit def jsonWritesApiDocOrganizationMetadata: play.api.libs.json.Writes[OrganizationMetadata] = new play.api.libs.json.Writes[OrganizationMetadata] {
      def writes(x: OrganizationMetadata) = play.api.libs.json.Json.obj(
        "package_name" -> play.api.libs.json.Json.toJson(x.packageName)
      )
    }

    implicit def jsonReadsApiDocService: play.api.libs.json.Reads[Service] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "name").read[String] and
        (__ \ "key").read[String] and
        (__ \ "visibility").read[Visibility] and
        (__ \ "description").readNullable[String]
      )(Service.apply _)
    }

    implicit def jsonWritesApiDocService: play.api.libs.json.Writes[Service] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "name").write[String] and
        (__ \ "key").write[String] and
        (__ \ "visibility").write[Visibility] and
        (__ \ "description").write[scala.Option[String]]
      )(unlift(Service.unapply _))
    }

    implicit def jsonReadsApiDocUser: play.api.libs.json.Reads[User] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "email").read[String] and
        (__ \ "name").readNullable[String]
      )(User.apply _)
    }

    implicit def jsonWritesApiDocUser: play.api.libs.json.Writes[User] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "email").write[String] and
        (__ \ "name").write[scala.Option[String]]
      )(unlift(User.unapply _))
    }

    implicit def jsonReadsApiDocValidation: play.api.libs.json.Reads[Validation] = {
      (
        (__ \ "valid").read[Boolean] and
        (__ \ "errors").readNullable[scala.collection.Seq[String]].map(_.getOrElse(Nil))
      )(Validation.apply _)
    }

    implicit def jsonWritesApiDocValidation: play.api.libs.json.Writes[Validation] = {
      (
        (__ \ "valid").write[Boolean] and
        (__ \ "errors").write[scala.collection.Seq[String]]
      )(unlift(Validation.unapply _))
    }

    implicit def jsonReadsApiDocVersion: play.api.libs.json.Reads[Version] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "version").read[String] and
        (__ \ "json").read[String]
      )(Version.apply _)
    }

    implicit def jsonWritesApiDocVersion: play.api.libs.json.Writes[Version] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "version").write[String] and
        (__ \ "json").write[String]
      )(unlift(Version.unapply _))
    }
  }
}
