package com.gilt.apidoc.models {
  /**
   * Generated source code.
   */
  case class Code(
    generator: com.gilt.apidoc.models.Generator,
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

  /**
   * An apidoc generator
   */
  case class Generator(
    guid: _root_.java.util.UUID,
    key: String,
    uri: String,
    name: String,
    language: scala.Option[String] = None,
    description: scala.Option[String] = None,
    visibility: com.gilt.apidoc.models.Visibility,
    owner: com.gilt.apidoc.models.User,
    enabled: Boolean
  )

  /**
   * Form to create a new generator
   */
  case class GeneratorCreateForm(
    key: String,
    uri: String,
    visibility: com.gilt.apidoc.models.Visibility
  )

  /**
   * Form to enable or disable a generator for an organization
   */
  case class GeneratorOrgForm(
    enabled: Boolean
  )

  /**
   * Form to update a generator
   */
  case class GeneratorUpdateForm(
    visibility: scala.Option[com.gilt.apidoc.models.Visibility] = None,
    enabled: scala.Option[Boolean] = None
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
    guid: _root_.java.util.UUID,
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
    guid: _root_.java.util.UUID,
    user: com.gilt.apidoc.models.User,
    organization: com.gilt.apidoc.models.Organization,
    role: String
  )

  /**
   * An organization is used to group a set of services together.
   */
  case class Organization(
    guid: _root_.java.util.UUID,
    key: String,
    name: String,
    domains: Seq[com.gilt.apidoc.models.Domain] = Nil,
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
    guid: _root_.java.util.UUID,
    name: String,
    key: String,
    visibility: com.gilt.apidoc.models.Visibility,
    description: scala.Option[String] = None
  )

  /**
   * A user is a top level person interacting with the api doc server.
   */
  case class User(
    guid: _root_.java.util.UUID,
    email: String,
    name: scala.Option[String] = None
  )

  /**
   * Used only to validate json files - used as a resource where http status code
   * defines success
   */
  case class Validation(
    valid: Boolean,
    errors: Seq[String] = Nil
  )

  /**
   * Represents a unique version of the service.
   */
  case class Version(
    guid: _root_.java.util.UUID,
    version: String,
    json: String
  )

  /**
   * Controls who is able to view this version
   */
  sealed trait Visibility

  object Visibility {

    /**
     * Only the creator can view this service
     */
    case object User extends Visibility { override def toString = "user" }
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
    val all = Seq(User, Organization, Public)

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
        (__ \ "generator").read[com.gilt.apidoc.models.Generator] and
        (__ \ "source").read[String]
      )(Code.apply _)
    }

    implicit def jsonWritesApidocCode: play.api.libs.json.Writes[Code] = {
      (
        (__ \ "generator").write[com.gilt.apidoc.models.Generator] and
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

    implicit def jsonReadsApidocGenerator: play.api.libs.json.Reads[Generator] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "key").read[String] and
        (__ \ "uri").read[String] and
        (__ \ "name").read[String] and
        (__ \ "language").readNullable[String] and
        (__ \ "description").readNullable[String] and
        (__ \ "visibility").read[com.gilt.apidoc.models.Visibility] and
        (__ \ "owner").read[com.gilt.apidoc.models.User] and
        (__ \ "enabled").read[Boolean]
      )(Generator.apply _)
    }

    implicit def jsonWritesApidocGenerator: play.api.libs.json.Writes[Generator] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "key").write[String] and
        (__ \ "uri").write[String] and
        (__ \ "name").write[String] and
        (__ \ "language").write[scala.Option[String]] and
        (__ \ "description").write[scala.Option[String]] and
        (__ \ "visibility").write[com.gilt.apidoc.models.Visibility] and
        (__ \ "owner").write[com.gilt.apidoc.models.User] and
        (__ \ "enabled").write[Boolean]
      )(unlift(Generator.unapply _))
    }

    implicit def jsonReadsApidocGeneratorCreateForm: play.api.libs.json.Reads[GeneratorCreateForm] = {
      (
        (__ \ "key").read[String] and
        (__ \ "uri").read[String] and
        (__ \ "visibility").read[com.gilt.apidoc.models.Visibility]
      )(GeneratorCreateForm.apply _)
    }

    implicit def jsonWritesApidocGeneratorCreateForm: play.api.libs.json.Writes[GeneratorCreateForm] = {
      (
        (__ \ "key").write[String] and
        (__ \ "uri").write[String] and
        (__ \ "visibility").write[com.gilt.apidoc.models.Visibility]
      )(unlift(GeneratorCreateForm.unapply _))
    }

    implicit def jsonReadsApidocGeneratorOrgForm: play.api.libs.json.Reads[GeneratorOrgForm] = {
      (__ \ "enabled").read[Boolean].map { x => new GeneratorOrgForm(enabled = x) }
    }

    implicit def jsonWritesApidocGeneratorOrgForm: play.api.libs.json.Writes[GeneratorOrgForm] = new play.api.libs.json.Writes[GeneratorOrgForm] {
      def writes(x: GeneratorOrgForm) = play.api.libs.json.Json.obj(
        "enabled" -> play.api.libs.json.Json.toJson(x.enabled)
      )
    }

    implicit def jsonReadsApidocGeneratorUpdateForm: play.api.libs.json.Reads[GeneratorUpdateForm] = {
      (
        (__ \ "visibility").readNullable[com.gilt.apidoc.models.Visibility] and
        (__ \ "enabled").readNullable[Boolean]
      )(GeneratorUpdateForm.apply _)
    }

    implicit def jsonWritesApidocGeneratorUpdateForm: play.api.libs.json.Writes[GeneratorUpdateForm] = {
      (
        (__ \ "visibility").write[scala.Option[com.gilt.apidoc.models.Visibility]] and
        (__ \ "enabled").write[scala.Option[Boolean]]
      )(unlift(GeneratorUpdateForm.unapply _))
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
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "user").read[com.gilt.apidoc.models.User] and
        (__ \ "organization").read[com.gilt.apidoc.models.Organization] and
        (__ \ "role").read[String]
      )(Membership.apply _)
    }

    implicit def jsonWritesApidocMembership: play.api.libs.json.Writes[Membership] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "user").write[com.gilt.apidoc.models.User] and
        (__ \ "organization").write[com.gilt.apidoc.models.Organization] and
        (__ \ "role").write[String]
      )(unlift(Membership.unapply _))
    }

    implicit def jsonReadsApidocMembershipRequest: play.api.libs.json.Reads[MembershipRequest] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "user").read[com.gilt.apidoc.models.User] and
        (__ \ "organization").read[com.gilt.apidoc.models.Organization] and
        (__ \ "role").read[String]
      )(MembershipRequest.apply _)
    }

    implicit def jsonWritesApidocMembershipRequest: play.api.libs.json.Writes[MembershipRequest] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "user").write[com.gilt.apidoc.models.User] and
        (__ \ "organization").write[com.gilt.apidoc.models.Organization] and
        (__ \ "role").write[String]
      )(unlift(MembershipRequest.unapply _))
    }

    implicit def jsonReadsApidocOrganization: play.api.libs.json.Reads[Organization] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "key").read[String] and
        (__ \ "name").read[String] and
        (__ \ "domains").readNullable[Seq[com.gilt.apidoc.models.Domain]].map(_.getOrElse(Nil)) and
        (__ \ "metadata").readNullable[com.gilt.apidoc.models.OrganizationMetadata]
      )(Organization.apply _)
    }

    implicit def jsonWritesApidocOrganization: play.api.libs.json.Writes[Organization] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "key").write[String] and
        (__ \ "name").write[String] and
        (__ \ "domains").write[Seq[com.gilt.apidoc.models.Domain]] and
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
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "name").read[String] and
        (__ \ "key").read[String] and
        (__ \ "visibility").read[com.gilt.apidoc.models.Visibility] and
        (__ \ "description").readNullable[String]
      )(Service.apply _)
    }

    implicit def jsonWritesApidocService: play.api.libs.json.Writes[Service] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "name").write[String] and
        (__ \ "key").write[String] and
        (__ \ "visibility").write[com.gilt.apidoc.models.Visibility] and
        (__ \ "description").write[scala.Option[String]]
      )(unlift(Service.unapply _))
    }

    implicit def jsonReadsApidocUser: play.api.libs.json.Reads[User] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "email").read[String] and
        (__ \ "name").readNullable[String]
      )(User.apply _)
    }

    implicit def jsonWritesApidocUser: play.api.libs.json.Writes[User] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "email").write[String] and
        (__ \ "name").write[scala.Option[String]]
      )(unlift(User.unapply _))
    }

    implicit def jsonReadsApidocValidation: play.api.libs.json.Reads[Validation] = {
      (
        (__ \ "valid").read[Boolean] and
        (__ \ "errors").readNullable[Seq[String]].map(_.getOrElse(Nil))
      )(Validation.apply _)
    }

    implicit def jsonWritesApidocValidation: play.api.libs.json.Writes[Validation] = {
      (
        (__ \ "valid").write[Boolean] and
        (__ \ "errors").write[Seq[String]]
      )(unlift(Validation.unapply _))
    }

    implicit def jsonReadsApidocVersion: play.api.libs.json.Reads[Version] = {
      (
        (__ \ "guid").read[_root_.java.util.UUID] and
        (__ \ "version").read[String] and
        (__ \ "json").read[String]
      )(Version.apply _)
    }

    implicit def jsonWritesApidocVersion: play.api.libs.json.Writes[Version] = {
      (
        (__ \ "guid").write[_root_.java.util.UUID] and
        (__ \ "version").write[String] and
        (__ \ "json").write[String]
      )(unlift(Version.unapply _))
    }
  }
}

package com.gilt.apidoc {

  class Client(apiUrl: String, apiToken: scala.Option[String] = None) {
    import com.gilt.apidoc.models.json._

    private val UserAgent = "apidoc:0.6.13 http://www.apidoc.me/gilt/code/apidoc/0.6.5/play_2_3_client"
    private val logger = play.api.Logger("com.gilt.apidoc.client")

    logger.info(s"Initializing com.gilt.apidoc.client for url $apiUrl")

    def code: Code = Code

    def domains: Domains = Domains

    def generators: Generators = Generators

    def healthchecks: Healthchecks = Healthchecks

    def membershipRequests: MembershipRequests = MembershipRequests

    def memberships: Memberships = Memberships

    def organizationMetadata: OrganizationMetadata = OrganizationMetadata

    def organizations: Organizations = Organizations

    def services: Services = Services

    def users: Users = Users

    def validations: Validations = Validations

    def versions: Versions = Versions

    object Code extends Code {
      override def getByOrgKeyAndServiceKeyAndVersionAndGeneratorKey(
        orgKey: String,
        serviceKey: String,
        version: String,
        generatorKey: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Code]] = {
        _executeRequest("GET", s"/${play.utils.UriEncoding.encodePathSegment(orgKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(serviceKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(version, "UTF-8")}/${generatorKey}").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.apidoc.models.Code])
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Domains extends Domains {
      override def post(domain: com.gilt.apidoc.models.Domain,
        orgKey: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Domain] = {
        val payload = play.api.libs.json.Json.toJson(domain)

        _executeRequest("POST", s"/domains/${play.utils.UriEncoding.encodePathSegment(orgKey, "UTF-8")}", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.Domain]
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def deleteByName(
        orgKey: String,
        name: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        _executeRequest("DELETE", s"/domains/${play.utils.UriEncoding.encodePathSegment(orgKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(name, "UTF-8")}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Generators extends Generators {
      override def get(
        guid: scala.Option[_root_.java.util.UUID] = None,
        key: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.Generator]] = {
        val queryParameters = Seq(
          guid.map("guid" -> _.toString),
          key.map("key" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        _executeRequest("GET", s"/generators", queryParameters = queryParameters).map {
          case r if r.status == 200 => r.json.as[Seq[com.gilt.apidoc.models.Generator]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getByKey(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Generator]] = {
        _executeRequest("GET", s"/generators/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.apidoc.models.Generator])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def post(generatorCreateForm: com.gilt.apidoc.models.GeneratorCreateForm)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Generator] = {
        val payload = play.api.libs.json.Json.toJson(generatorCreateForm)

        _executeRequest("POST", s"/generators", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.Generator]
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def putByKey(generatorUpdateForm: com.gilt.apidoc.models.GeneratorUpdateForm,
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Generator] = {
        val payload = play.api.libs.json.Json.toJson(generatorUpdateForm)

        _executeRequest("PUT", s"/generators/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.Generator]
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def putByKey(generatorOrgForm: com.gilt.apidoc.models.GeneratorOrgForm,
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Generator] = {
        val payload = play.api.libs.json.Json.toJson(generatorOrgForm)

        _executeRequest("PUT", s"/generators/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.Generator]
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def deleteByKey(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        _executeRequest("DELETE", s"/generators/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Healthchecks extends Healthchecks {
      override def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Healthcheck]] = {
        _executeRequest("GET", s"/_internal_/healthcheck").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.apidoc.models.Healthcheck])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    object MembershipRequests extends MembershipRequests {
      override def get(
        orgGuid: scala.Option[_root_.java.util.UUID] = None,
        orgKey: scala.Option[String] = None,
        userGuid: scala.Option[_root_.java.util.UUID] = None,
        role: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.MembershipRequest]] = {
        val queryParameters = Seq(
          orgGuid.map("org_guid" -> _.toString),
          orgKey.map("org_key" -> _),
          userGuid.map("user_guid" -> _.toString),
          role.map("role" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        _executeRequest("GET", s"/membership_requests", queryParameters = queryParameters).map {
          case r if r.status == 200 => r.json.as[Seq[com.gilt.apidoc.models.MembershipRequest]]
          case r => throw new FailedRequest(r)
        }
      }

      override def post(
        orgGuid: _root_.java.util.UUID,
        userGuid: _root_.java.util.UUID,
        role: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.MembershipRequest] = {
        val payload = play.api.libs.json.Json.obj(
          "org_guid" -> play.api.libs.json.Json.toJson(orgGuid),
          "user_guid" -> play.api.libs.json.Json.toJson(userGuid),
          "role" -> play.api.libs.json.Json.toJson(role)
        )

        _executeRequest("POST", s"/membership_requests", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.MembershipRequest]
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def postAcceptByGuid(
        guid: _root_.java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit] = {
        _executeRequest("POST", s"/membership_requests/${guid}/accept").map {
          case r if r.status == 204 => Unit
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def postDeclineByGuid(
        guid: _root_.java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit] = {
        _executeRequest("POST", s"/membership_requests/${guid}/decline").map {
          case r if r.status == 204 => Unit
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Memberships extends Memberships {
      override def get(
        orgGuid: scala.Option[_root_.java.util.UUID] = None,
        orgKey: scala.Option[String] = None,
        userGuid: scala.Option[_root_.java.util.UUID] = None,
        role: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.Membership]] = {
        val queryParameters = Seq(
          orgGuid.map("org_guid" -> _.toString),
          orgKey.map("org_key" -> _),
          userGuid.map("user_guid" -> _.toString),
          role.map("role" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        _executeRequest("GET", s"/memberships", queryParameters = queryParameters).map {
          case r if r.status == 200 => r.json.as[Seq[com.gilt.apidoc.models.Membership]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getByGuid(
        guid: _root_.java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Membership]] = {
        _executeRequest("GET", s"/memberships/${guid}").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.apidoc.models.Membership])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def deleteByGuid(
        guid: _root_.java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        _executeRequest("DELETE", s"/memberships/${guid}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    object OrganizationMetadata extends OrganizationMetadata {
      override def put(organizationMetadata: com.gilt.apidoc.models.OrganizationMetadata,
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.OrganizationMetadata] = {
        val payload = play.api.libs.json.Json.toJson(organizationMetadata)

        _executeRequest("PUT", s"/organizations/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}/metadata", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.OrganizationMetadata]
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Organizations extends Organizations {
      override def get(
        guid: scala.Option[_root_.java.util.UUID] = None,
        userGuid: scala.Option[_root_.java.util.UUID] = None,
        key: scala.Option[String] = None,
        name: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.Organization]] = {
        val queryParameters = Seq(
          guid.map("guid" -> _.toString),
          userGuid.map("user_guid" -> _.toString),
          key.map("key" -> _),
          name.map("name" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        _executeRequest("GET", s"/organizations", queryParameters = queryParameters).map {
          case r if r.status == 200 => r.json.as[Seq[com.gilt.apidoc.models.Organization]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getByKey(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Organization]] = {
        _executeRequest("GET", s"/organizations/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.apidoc.models.Organization])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def post(
        name: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Organization] = {
        val payload = play.api.libs.json.Json.obj(
          "name" -> play.api.libs.json.Json.toJson(name)
        )

        _executeRequest("POST", s"/organizations", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.Organization]
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def deleteByKey(
        key: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        _executeRequest("DELETE", s"/organizations/${play.utils.UriEncoding.encodePathSegment(key, "UTF-8")}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Services extends Services {
      override def getByOrgKey(
        orgKey: String,
        name: scala.Option[String] = None,
        key: scala.Option[String] = None,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.Service]] = {
        val queryParameters = Seq(
          name.map("name" -> _),
          key.map("key" -> _),
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        _executeRequest("GET", s"/${play.utils.UriEncoding.encodePathSegment(orgKey, "UTF-8")}", queryParameters = queryParameters).map {
          case r if r.status == 200 => r.json.as[Seq[com.gilt.apidoc.models.Service]]
          case r => throw new FailedRequest(r)
        }
      }

      override def putByOrgKeyAndServiceKey(
        orgKey: String,
        serviceKey: String,
        name: String,
        description: scala.Option[String] = None,
        visibility: com.gilt.apidoc.models.Visibility
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit] = {
        val payload = play.api.libs.json.Json.obj(
          "name" -> play.api.libs.json.Json.toJson(name),
          "description" -> play.api.libs.json.Json.toJson(description),
          "visibility" -> play.api.libs.json.Json.toJson(visibility)
        )

        _executeRequest("PUT", s"/${play.utils.UriEncoding.encodePathSegment(orgKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(serviceKey, "UTF-8")}", body = Some(payload)).map {
          case r if r.status == 204 => Unit
          case r => throw new FailedRequest(r)
        }
      }

      override def deleteByOrgKeyAndServiceKey(
        orgKey: String,
        serviceKey: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        _executeRequest("DELETE", s"/${play.utils.UriEncoding.encodePathSegment(orgKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(serviceKey, "UTF-8")}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Users extends Users {
      override def get(
        guid: scala.Option[_root_.java.util.UUID] = None,
        email: scala.Option[String] = None,
        token: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.User]] = {
        val queryParameters = Seq(
          guid.map("guid" -> _.toString),
          email.map("email" -> _),
          token.map("token" -> _)
        ).flatten

        _executeRequest("GET", s"/users", queryParameters = queryParameters).map {
          case r if r.status == 200 => r.json.as[Seq[com.gilt.apidoc.models.User]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getByGuid(
        guid: _root_.java.util.UUID
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.User]] = {
        _executeRequest("GET", s"/users/${guid}").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.apidoc.models.User])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def postAuthenticate(
        email: String,
        password: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.User] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "password" -> play.api.libs.json.Json.toJson(password)
        )

        _executeRequest("POST", s"/users/authenticate", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.User]
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def post(
        email: String,
        name: scala.Option[String] = None,
        password: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.User] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "name" -> play.api.libs.json.Json.toJson(name),
          "password" -> play.api.libs.json.Json.toJson(password)
        )

        _executeRequest("POST", s"/users", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.User]
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }

      override def putByGuid(
        guid: _root_.java.util.UUID,
        email: String,
        name: scala.Option[String] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.User] = {
        val payload = play.api.libs.json.Json.obj(
          "email" -> play.api.libs.json.Json.toJson(email),
          "name" -> play.api.libs.json.Json.toJson(name)
        )

        _executeRequest("PUT", s"/users/${guid}", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.User]
          case r if r.status == 409 => throw new com.gilt.apidoc.error.ErrorsResponse(r)
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Validations extends Validations {
      override def post(value: String)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Validation] = {
        val payload = play.api.libs.json.Json.toJson(value)

        _executeRequest("POST", s"/validations", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.Validation]
          case r => throw new FailedRequest(r)
        }
      }
    }

    object Versions extends Versions {
      override def getByOrgKeyAndServiceKey(
        orgKey: String,
        serviceKey: String,
        limit: scala.Option[Int] = None,
        offset: scala.Option[Int] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.Version]] = {
        val queryParameters = Seq(
          limit.map("limit" -> _.toString),
          offset.map("offset" -> _.toString)
        ).flatten

        _executeRequest("GET", s"/${play.utils.UriEncoding.encodePathSegment(orgKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(serviceKey, "UTF-8")}", queryParameters = queryParameters).map {
          case r if r.status == 200 => r.json.as[Seq[com.gilt.apidoc.models.Version]]
          case r => throw new FailedRequest(r)
        }
      }

      override def getByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Version]] = {
        _executeRequest("GET", s"/${play.utils.UriEncoding.encodePathSegment(orgKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(serviceKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(version, "UTF-8")}").map {
          case r if r.status == 200 => Some(r.json.as[com.gilt.apidoc.models.Version])
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }

      override def putByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String,
        json: String,
        visibility: scala.Option[com.gilt.apidoc.models.Visibility] = None
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Version] = {
        val payload = play.api.libs.json.Json.obj(
          "json" -> play.api.libs.json.Json.toJson(json),
          "visibility" -> play.api.libs.json.Json.toJson(visibility)
        )

        _executeRequest("PUT", s"/${play.utils.UriEncoding.encodePathSegment(orgKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(serviceKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(version, "UTF-8")}", body = Some(payload)).map {
          case r if r.status == 200 => r.json.as[com.gilt.apidoc.models.Version]
          case r => throw new FailedRequest(r)
        }
      }

      override def deleteByOrgKeyAndServiceKeyAndVersion(
        orgKey: String,
        serviceKey: String,
        version: String
      )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]] = {
        _executeRequest("DELETE", s"/${play.utils.UriEncoding.encodePathSegment(orgKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(serviceKey, "UTF-8")}/${play.utils.UriEncoding.encodePathSegment(version, "UTF-8")}").map {
          case r if r.status == 204 => Some(Unit)
          case r if r.status == 404 => None
          case r => throw new FailedRequest(r)
        }
      }
    }

    def _requestHolder(path: String): play.api.libs.ws.WSRequestHolder = {
      import play.api.Play.current

      val holder = play.api.libs.ws.WS.url(apiUrl + path).withHeaders("X-Api-Doc" -> "www.apidoc.me", "User-Agent" -> UserAgent)
      apiToken.fold(holder) { token =>
        holder.withAuth(token, "", play.api.libs.ws.WSAuthScheme.BASIC)
      }
    }

    def _logRequest(method: String, req: play.api.libs.ws.WSRequestHolder)(implicit ec: scala.concurrent.ExecutionContext): play.api.libs.ws.WSRequestHolder = {
      val queryComponents = for {
        (name, values) <- req.queryString
        value <- values
      } yield name -> value
      val url = s"${req.url}${queryComponents.mkString("?", "&", "")}"
      apiToken.fold(logger.info(s"curl -X $method $url")) { _ =>
        logger.info(s"curl -X $method -u '[REDACTED]:' $url")
      }
      req
    }

    def _executeRequest(
      method: String,
      path: String,
      queryParameters: Seq[(String, String)] = Seq.empty,
      body: Option[play.api.libs.json.JsValue] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[play.api.libs.ws.WSResponse] = {
      method.toUpperCase match {
        case "GET" => {
          _logRequest("GET", _requestHolder(path).withQueryString(queryParameters:_*)).get()
        }
        case "POST" => {
          _logRequest("POST", _requestHolder(path).withQueryString(queryParameters:_*)).post(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PUT" => {
          _logRequest("PUT", _requestHolder(path).withQueryString(queryParameters:_*)).put(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "PATCH" => {
          _logRequest("PATCH", _requestHolder(path).withQueryString(queryParameters:_*)).patch(body.getOrElse(play.api.libs.json.Json.obj()))
        }
        case "DELETE" => {
          _logRequest("DELETE", _requestHolder(path).withQueryString(queryParameters:_*)).delete()
        }
         case "HEAD" => {
          _logRequest("HEAD", _requestHolder(path).withQueryString(queryParameters:_*)).head()
        }
        case _ => {
          _logRequest(method, _requestHolder(path).withQueryString(queryParameters:_*))
          sys.error("Unsupported method[%s]".format(method))
        }
      }
    }

  }

  trait Code {
    /**
     * Generate code for a specific version of a service.
     */
    def getByOrgKeyAndServiceKeyAndVersionAndGeneratorKey(
      orgKey: String,
      serviceKey: String,
      version: String,
      generatorKey: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Code]]
  }

  trait Domains {
    /**
     * Add a domain to this organization
     */
    def post(domain: com.gilt.apidoc.models.Domain,
      orgKey: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Domain]

    /**
     * Remove this domain from this organization
     */
    def deleteByName(
      orgKey: String,
      name: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
  }

  trait Generators {
    /**
     * List all generators visible by this user
     */
    def get(
      guid: scala.Option[_root_.java.util.UUID] = None,
      key: scala.Option[String] = None,
      limit: scala.Option[Int] = None,
      offset: scala.Option[Int] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.Generator]]

    def getByKey(
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Generator]]

    def post(generatorCreateForm: com.gilt.apidoc.models.GeneratorCreateForm)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Generator]

    def putByKey(generatorUpdateForm: com.gilt.apidoc.models.GeneratorUpdateForm,
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Generator]

    def putByKey(generatorOrgForm: com.gilt.apidoc.models.GeneratorOrgForm,
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Generator]

    def deleteByKey(
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
  }

  trait Healthchecks {
    def get()(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Healthcheck]]
  }

  trait MembershipRequests {
    /**
     * Search all membership requests. Results are always paginated.
     */
    def get(
      orgGuid: scala.Option[_root_.java.util.UUID] = None,
      orgKey: scala.Option[String] = None,
      userGuid: scala.Option[_root_.java.util.UUID] = None,
      role: scala.Option[String] = None,
      limit: scala.Option[Int] = None,
      offset: scala.Option[Int] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.MembershipRequest]]

    /**
     * Create a membership request
     */
    def post(
      orgGuid: _root_.java.util.UUID,
      userGuid: _root_.java.util.UUID,
      role: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.MembershipRequest]

    /**
     * Accepts this membership request. User will become a member of the specified
     * organization.
     */
    def postAcceptByGuid(
      guid: _root_.java.util.UUID
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit]

    /**
     * Declines this membership request. User will NOT become a member of the specified
     * organization.
     */
    def postDeclineByGuid(
      guid: _root_.java.util.UUID
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit]
  }

  trait Memberships {
    /**
     * Search all memberships. Results are always paginated.
     */
    def get(
      orgGuid: scala.Option[_root_.java.util.UUID] = None,
      orgKey: scala.Option[String] = None,
      userGuid: scala.Option[_root_.java.util.UUID] = None,
      role: scala.Option[String] = None,
      limit: scala.Option[Int] = None,
      offset: scala.Option[Int] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.Membership]]

    def getByGuid(
      guid: _root_.java.util.UUID
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Membership]]

    def deleteByGuid(
      guid: _root_.java.util.UUID
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
  }

  trait OrganizationMetadata {
    /**
     * Update metadata for this organization
     */
    def put(organizationMetadata: com.gilt.apidoc.models.OrganizationMetadata,
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.OrganizationMetadata]
  }

  trait Organizations {
    /**
     * Search all organizations. Results are always paginated.
     */
    def get(
      guid: scala.Option[_root_.java.util.UUID] = None,
      userGuid: scala.Option[_root_.java.util.UUID] = None,
      key: scala.Option[String] = None,
      name: scala.Option[String] = None,
      limit: scala.Option[Int] = None,
      offset: scala.Option[Int] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.Organization]]

    /**
     * Returns the organization with this key.
     */
    def getByKey(
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Organization]]

    /**
     * Create a new organization.
     */
    def post(
      name: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Organization]

    /**
     * Deletes an organization and all of its associated services.
     */
    def deleteByKey(
      key: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
  }

  trait Services {
    /**
     * Search all services. Results are always paginated.
     */
    def getByOrgKey(
      orgKey: String,
      name: scala.Option[String] = None,
      key: scala.Option[String] = None,
      limit: scala.Option[Int] = None,
      offset: scala.Option[Int] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.Service]]

    /**
     * Updates a service.
     */
    def putByOrgKeyAndServiceKey(
      orgKey: String,
      serviceKey: String,
      name: String,
      description: scala.Option[String] = None,
      visibility: com.gilt.apidoc.models.Visibility
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Unit]

    /**
     * Deletes a specific service and its associated versions.
     */
    def deleteByOrgKeyAndServiceKey(
      orgKey: String,
      serviceKey: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
  }

  trait Users {
    /**
     * Search for a specific user. You must specify at least 1 parameter - either a
     * guid, email or token - and will receive back either 0 or 1 users.
     */
    def get(
      guid: scala.Option[_root_.java.util.UUID] = None,
      email: scala.Option[String] = None,
      token: scala.Option[String] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.User]]

    /**
     * Returns information about the user with this guid.
     */
    def getByGuid(
      guid: _root_.java.util.UUID
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.User]]

    /**
     * Used to authenticate a user with an email address and password. Successful
     * authentication returns an instance of the user model. Failed authorizations of
     * any kind are returned as a generic error with code user_authorization_failed.
     */
    def postAuthenticate(
      email: String,
      password: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.User]

    /**
     * Create a new user.
     */
    def post(
      email: String,
      name: scala.Option[String] = None,
      password: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.User]

    /**
     * Updates information about the user with the specified guid.
     */
    def putByGuid(
      guid: _root_.java.util.UUID,
      email: String,
      name: scala.Option[String] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.User]
  }

  trait Validations {
    def post(value: String)(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Validation]
  }

  trait Versions {
    /**
     * Search all versions of this service. Results are always paginated.
     */
    def getByOrgKeyAndServiceKey(
      orgKey: String,
      serviceKey: String,
      limit: scala.Option[Int] = None,
      offset: scala.Option[Int] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[Seq[com.gilt.apidoc.models.Version]]

    /**
     * Retrieve a specific version of a service.
     */
    def getByOrgKeyAndServiceKeyAndVersion(
      orgKey: String,
      serviceKey: String,
      version: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[com.gilt.apidoc.models.Version]]

    /**
     * Create or update the service with the specified version.
     */
    def putByOrgKeyAndServiceKeyAndVersion(
      orgKey: String,
      serviceKey: String,
      version: String,
      json: String,
      visibility: scala.Option[com.gilt.apidoc.models.Visibility] = None
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[com.gilt.apidoc.models.Version]

    /**
     * Deletes a specific version.
     */
    def deleteByOrgKeyAndServiceKeyAndVersion(
      orgKey: String,
      serviceKey: String,
      version: String
    )(implicit ec: scala.concurrent.ExecutionContext): scala.concurrent.Future[scala.Option[Unit]]
  }

  case class FailedRequest(
    response: play.api.libs.ws.WSResponse,
    message: Option[String] = None
  ) extends Exception(message.getOrElse(response.status + ": " + response.body))

  package error {

    import com.gilt.apidoc.models.json._

    case class ErrorsResponse(
      response: play.api.libs.ws.WSResponse,
      message: Option[String] = None
    ) extends Exception(message.getOrElse(response.status + ": " + response.body)){
      import com.gilt.apidoc.models.json._
      lazy val errors = response.json.as[Seq[com.gilt.apidoc.models.Error]]
    }
  }

  object Bindables {

    import play.api.mvc.{PathBindable, QueryStringBindable}
    import org.joda.time.{DateTime, LocalDate}
    import org.joda.time.format.ISODateTimeFormat
    import com.gilt.apidoc.models._

    // Type: date-time-iso8601
    implicit val pathBindableTypeDateTimeIso8601 = new PathBindable.Parsing[DateTime](
      ISODateTimeFormat.dateTimeParser.parseDateTime(_), _.toString, (key: String, e: Exception) => s"Error parsing date time $key. Example: 2014-04-29T11:56:52Z"
    )

    // Type: date-iso8601
    implicit val pathBindableTypeDateIso8601 = new PathBindable.Parsing[LocalDate](
      ISODateTimeFormat.yearMonthDay.parseLocalDate(_), _.toString, (key: String, e: Exception) => s"Error parsing date $key. Example: 2014-04-29"
    )

    // Enum: Visibility
    private val enumVisibilityNotFound = (key: String, e: Exception) => s"Unrecognized $key, should be one of ${Visibility.all.mkString(", ")}"

    implicit val pathBindableEnumVisibility = new PathBindable.Parsing[Visibility] (
      Visibility.fromString(_).get, _.toString, enumVisibilityNotFound
    )

    implicit val queryStringBindableEnumVisibility = new QueryStringBindable.Parsing[Visibility](
      Visibility.fromString(_).get, _.toString, enumVisibilityNotFound
    )

  }

}
