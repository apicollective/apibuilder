package com.gilt.apidoc.models {
  /**
   * Generated source code.
   */
  case class Code(
    targetKey: String,
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
    user: com.gilt.apidoc.models.User,
    organization: com.gilt.apidoc.models.Organization,
    role: String
  )

  /**
   * A membership request represents a user requesting to join an organization with a
   * specific role (e.g. as a member or an admin). Membership requests can be
   * reviewed by any current admin of the organization who can either accept or
   * decline the request.
   */
  case class MembershipRequest(
    guid: java.util.UUID,
    user: com.gilt.apidoc.models.User,
    organization: com.gilt.apidoc.models.Organization,
    role: String
  )

  /**
   * An organization is used to group a set of services together.
   */
  case class Organization(
    guid: java.util.UUID,
    key: String,
    name: String,
    domains: scala.collection.Seq[com.gilt.apidoc.models.Domain] = Nil,
    metadata: scala.Option[com.gilt.apidoc.models.OrganizationMetadata] = None
  )

  /**
   * Supplemental (non-required) information about an organization
   */
  case class OrganizationMetadata(
    visibility: scala.Option[com.gilt.apidoc.models.Visibility] = None,
    packageName: scala.Option[String] = None
  )

  /**
   * A service has a name and multiple versions of an API (Interface).
   */
  case class Service(
    guid: java.util.UUID,
    name: String,
    key: String,
    visibility: com.gilt.apidoc.models.Visibility,
    description: scala.Option[String] = None
  )

  /**
   * The target platform for code generation.
   */
  case class Target(
    key: String,
    name: String,
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

    implicit val jsonReadsApidocEnum_Visibility = __.read[String].map(Visibility.apply)
    implicit val jsonWritesApidocEnum_Visibility = new Writes[Visibility] {
      def writes(x: Visibility) = JsString(x.toString)
    }
    implicit def jsonReadsApidocCode: play.api.libs.json.Reads[Code] = {
      (
        (__ \ "targetKey").read[String] and
        (__ \ "source").read[String]
      )(Code.apply _)
    }

    implicit def jsonWritesApidocCode: play.api.libs.json.Writes[Code] = {
      (
        (__ \ "targetKey").write[String] and
        (__ \ "source").write[String]
      )(unlift(Code.unapply _))
    }

    implicit def jsonReadsApidocDomain: play.api.libs.json.Reads[Domain] = {
      (__ \ "name").read[String].map { x => new Domain(name = x) }
    }

    implicit def jsonWritesApidocDomain: play.api.libs.json.Writes[Domain] = new play.api.libs.json.Writes[Domain] {
      def writes(x: Domain) = play.api.libs.json.Json.obj(
        "name" -> play.api.libs.json.Json.toJson(x.name)
      )
    }

    implicit def jsonReadsApidocError: play.api.libs.json.Reads[Error] = {
      (
        (__ \ "code").read[String] and
        (__ \ "message").read[String]
      )(Error.apply _)
    }

    implicit def jsonWritesApidocError: play.api.libs.json.Writes[Error] = {
      (
        (__ \ "code").write[String] and
        (__ \ "message").write[String]
      )(unlift(Error.unapply _))
    }

    implicit def jsonReadsApidocHealthcheck: play.api.libs.json.Reads[Healthcheck] = {
      (__ \ "status").read[String].map { x => new Healthcheck(status = x) }
    }

    implicit def jsonWritesApidocHealthcheck: play.api.libs.json.Writes[Healthcheck] = new play.api.libs.json.Writes[Healthcheck] {
      def writes(x: Healthcheck) = play.api.libs.json.Json.obj(
        "status" -> play.api.libs.json.Json.toJson(x.status)
      )
    }

    implicit def jsonReadsApidocMembership: play.api.libs.json.Reads[Membership] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "user").read[com.gilt.apidoc.models.User] and
        (__ \ "organization").read[com.gilt.apidoc.models.Organization] and
        (__ \ "role").read[String]
      )(Membership.apply _)
    }

    implicit def jsonWritesApidocMembership: play.api.libs.json.Writes[Membership] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "user").write[com.gilt.apidoc.models.User] and
        (__ \ "organization").write[com.gilt.apidoc.models.Organization] and
        (__ \ "role").write[String]
      )(unlift(Membership.unapply _))
    }

    implicit def jsonReadsApidocMembershipRequest: play.api.libs.json.Reads[MembershipRequest] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "user").read[com.gilt.apidoc.models.User] and
        (__ \ "organization").read[com.gilt.apidoc.models.Organization] and
        (__ \ "role").read[String]
      )(MembershipRequest.apply _)
    }

    implicit def jsonWritesApidocMembershipRequest: play.api.libs.json.Writes[MembershipRequest] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "user").write[com.gilt.apidoc.models.User] and
        (__ \ "organization").write[com.gilt.apidoc.models.Organization] and
        (__ \ "role").write[String]
      )(unlift(MembershipRequest.unapply _))
    }

    implicit def jsonReadsApidocOrganization: play.api.libs.json.Reads[Organization] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "key").read[String] and
        (__ \ "name").read[String] and
        (__ \ "domains").readNullable[scala.collection.Seq[com.gilt.apidoc.models.Domain]].map(_.getOrElse(Nil)) and
        (__ \ "metadata").readNullable[com.gilt.apidoc.models.OrganizationMetadata]
      )(Organization.apply _)
    }

    implicit def jsonWritesApidocOrganization: play.api.libs.json.Writes[Organization] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "key").write[String] and
        (__ \ "name").write[String] and
        (__ \ "domains").write[scala.collection.Seq[com.gilt.apidoc.models.Domain]] and
        (__ \ "metadata").write[scala.Option[com.gilt.apidoc.models.OrganizationMetadata]]
      )(unlift(Organization.unapply _))
    }

    implicit def jsonReadsApidocOrganizationMetadata: play.api.libs.json.Reads[OrganizationMetadata] = {
      (
        (__ \ "visibility").readNullable[com.gilt.apidoc.models.Visibility] and
        (__ \ "package_name").readNullable[String]
      )(OrganizationMetadata.apply _)
    }

    implicit def jsonWritesApidocOrganizationMetadata: play.api.libs.json.Writes[OrganizationMetadata] = {
      (
        (__ \ "visibility").write[scala.Option[com.gilt.apidoc.models.Visibility]] and
        (__ \ "package_name").write[scala.Option[String]]
      )(unlift(OrganizationMetadata.unapply _))
    }

    implicit def jsonReadsApidocService: play.api.libs.json.Reads[Service] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "name").read[String] and
        (__ \ "key").read[String] and
        (__ \ "visibility").read[com.gilt.apidoc.models.Visibility] and
        (__ \ "description").readNullable[String]
      )(Service.apply _)
    }

    implicit def jsonWritesApidocService: play.api.libs.json.Writes[Service] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "name").write[String] and
        (__ \ "key").write[String] and
        (__ \ "visibility").write[com.gilt.apidoc.models.Visibility] and
        (__ \ "description").write[scala.Option[String]]
      )(unlift(Service.unapply _))
    }

    implicit def jsonReadsApidocTarget: play.api.libs.json.Reads[Target] = {
      (
        (__ \ "key").read[String] and
        (__ \ "name").read[String] and
        (__ \ "description").readNullable[String]
      )(Target.apply _)
    }

    implicit def jsonWritesApidocTarget: play.api.libs.json.Writes[Target] = {
      (
        (__ \ "key").write[String] and
        (__ \ "name").write[String] and
        (__ \ "description").write[scala.Option[String]]
      )(unlift(Target.unapply _))
    }

    implicit def jsonReadsApidocUser: play.api.libs.json.Reads[User] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "email").read[String] and
        (__ \ "name").readNullable[String]
      )(User.apply _)
    }

    implicit def jsonWritesApidocUser: play.api.libs.json.Writes[User] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "email").write[String] and
        (__ \ "name").write[scala.Option[String]]
      )(unlift(User.unapply _))
    }

    implicit def jsonReadsApidocValidation: play.api.libs.json.Reads[Validation] = {
      (
        (__ \ "valid").read[Boolean] and
        (__ \ "errors").readNullable[scala.collection.Seq[String]].map(_.getOrElse(Nil))
      )(Validation.apply _)
    }

    implicit def jsonWritesApidocValidation: play.api.libs.json.Writes[Validation] = {
      (
        (__ \ "valid").write[Boolean] and
        (__ \ "errors").write[scala.collection.Seq[String]]
      )(unlift(Validation.unapply _))
    }

    implicit def jsonReadsApidocVersion: play.api.libs.json.Reads[Version] = {
      (
        (__ \ "guid").read[java.util.UUID] and
        (__ \ "version").read[String] and
        (__ \ "json").read[String]
      )(Version.apply _)
    }

    implicit def jsonWritesApidocVersion: play.api.libs.json.Writes[Version] = {
      (
        (__ \ "guid").write[java.util.UUID] and
        (__ \ "version").write[String] and
        (__ \ "json").write[String]
      )(unlift(Version.unapply _))
    }
  }
}