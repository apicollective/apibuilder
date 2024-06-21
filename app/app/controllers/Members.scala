package controllers

import lib._
import io.apibuilder.api.v0.models.{Organization, User}
import io.apibuilder.common.v0.models.MembershipRole
import models._
import play.api.data._
import play.api.data.Forms._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import org.joda.time.DateTime

import java.util.UUID
import javax.inject.Inject

class Members @Inject() (
                          val apiBuilderControllerComponents: ApiBuilderControllerComponents,
                          apiClientProvider: ApiClientProvider
) extends ApiBuilderController {

  private[this] implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  def show(orgKey: String, page: Int = 0) = IdentifiedOrg.async { implicit request =>
    request.withMember {
      for {
        orgs <- request.api.Organizations.get(key = Some(orgKey))
        members <- request.api.Memberships.get(orgKey = Some(orgKey),
          limit = Pagination.DefaultLimit + 1,
          offset = page * Pagination.DefaultLimit)
        requests <- request.api.MembershipRequests.get(orgKey = Some(orgKey), limit = 1)
      } yield {
        orgs.headOption match {

          case None => Redirect("/").flashing("warning" -> "Organization not found")

          case Some(_) =>
            val tpl = request.mainTemplate(Some("Members")).copy(settings = Some(SettingsMenu(section = Some(SettingSection.Members))))
            Ok(views.html.members.show(tpl,
              members = PaginatedCollection(page, members),
              isAdmin = request.requestData.isAdmin,
              haveMembershipRequests = requests.nonEmpty
            )
            )
        }
      }
    }
  }

  def add(orgKey: String) = IdentifiedOrg { implicit request =>
    request.withMember {
      val filledForm = Members.addMemberForm.fill(Members.AddMemberData(role = MembershipRole.Member.toString, email = "", nickname = ""))

      val tpl = request.mainTemplate(Some("Add Member")).copy(settings = Some(SettingsMenu(section = Some(SettingSection.Members))))
      Ok(views.html.members.add(tpl, filledForm))
    }
  }

  def addPost(orgKey: String) = IdentifiedOrg { implicit request =>
    request.withMember {
      val tpl = request.mainTemplate(Some("Add Member")).copy(settings = Some(SettingsMenu(section = Some(SettingSection.Members))))

      Members.addMemberForm.bindFromRequest().fold(

        errors => {
          Ok(views.html.members.add(tpl, errors))
        },

        valid => {
          val filledForm = Members.addMemberForm.fill(valid)
          val email = toOption(valid.email)
          val nickname = toOption(valid.nickname)

          if (email.isDefined || nickname.isDefined) {
            Await.result(request.api.Users.get(email = email, nickname = nickname), 1500.millis).headOption match {

              case None => {
                Ok(views.html.members.add(tpl, filledForm, Some("No user found")))
              }

              case Some(user: User) => {
                val membershipRequest = Await.result(request.api.MembershipRequests.post(request.org.guid, user.guid, valid.role), 1500.millis)
                Await.result(request.api.MembershipRequests.postAcceptByGuid(membershipRequest.guid), 1500.millis)
                Redirect(routes.Members.show(request.org.key)).flashing("success" -> s"${valid.role} added")
              }
            }
          } else {
            Ok(views.html.members.add(tpl, filledForm, Some("Please enter either an email address or nickname")))
          }
        }

      )
    }
  }

  def postRemove(orgKey: String, guid: UUID) = IdentifiedOrg.async { implicit request =>
    request.withAdmin {
      for {
        _ <- request.api.Memberships.deleteByGuid(guid)
      } yield {
        Redirect(routes.Members.show(request.org.key)).flashing("success" -> s"Member removed")
      }
    }
  }

  def postRevokeAdmin(orgKey: String, guid: UUID) = IdentifiedOrg.async { implicit request =>
    request.withAdmin {
      for {
        membership <- apiClientProvider.callWith404(request.api.Memberships.getByGuid(guid))
        memberships <- request.api.Memberships.get(orgKey = Some(orgKey), userGuid = Some(membership.get.user.guid))
      } yield {
        def findByRole(r: MembershipRole) =memberships.find { m => MembershipRole(m.role) == r }
        findByRole(MembershipRole.Member) match {
          case None => createMembership(request.api, request.org, membership.get.user.guid, MembershipRole.Member)
          case Some(_) => // no-op
        }

        findByRole(MembershipRole.Admin) match {
          case None => // no-op
          case Some(m) => {
            Await.result(
              request.api.Memberships.deleteByGuid(m.guid),
              1500.millis
            )
          }
        }

        Redirect(routes.Members.show(request.org.key)).flashing("success" -> s"Member's admin access has been revoked")
      }
    }
  }

  def postMakeAdmin(orgKey: String, guid: UUID) = IdentifiedOrg.async { implicit request =>
    request.withAdmin {
      for {
        membership <- apiClientProvider.callWith404(request.api.Memberships.getByGuid(guid))
        memberships <- request.api.Memberships.get(orgKey = Some(orgKey), userGuid = Some(membership.get.user.guid))
      } yield {
        memberships.find(_.role == MembershipRole.Admin.toString) match {
          case None => createMembership(request.api, request.org, membership.get.user.guid, MembershipRole.Admin)
          case Some(_) => // no-op
        }

        memberships.find(_.role == MembershipRole.Member.toString) match {
          case None => // no-op
          case Some(m) => {
            Await.result(
              request.api.Memberships.deleteByGuid(m.guid),
              1500.millis
            )
          }
        }

        Redirect(routes.Members.show(request.org.key)).flashing("success" -> s"Member granted admin access")
      }
    }
  }

  def downloadCsv(orgKey: String) = IdentifiedOrg.async { implicit request =>
    request.withAdmin {
      for {
        path <- MemberDownload(request.api, orgKey).csv()
      } yield {
        val date = DateHelper.mediumDateTime(UserTimeZone(request.user), DateTime.now())
        Ok.sendFile(
          content = path,
          fileName = _ => Some(s"apibuilder-$orgKey-members-$date.csv")
        )
      }
    }
  }

  private[this] def createMembership(api: io.apibuilder.api.v0.Client, org: Organization, userGuid: UUID, role: MembershipRole): Unit = {
    val membershipRequest = Await.result(
      api.MembershipRequests.post(orgGuid = org.guid, userGuid = userGuid, role = role.toString),
      1500.millis
    )
    Await.result(
      api.MembershipRequests.postAcceptByGuid(membershipRequest.guid),
      1500.millis
    )
  }

  private[this] def toOption(value: String): Option[String] = {
    value.trim match {
      case "" => None
      case v => Some(v)
    }
  }
}

object Members {

  case class AddMemberData(role: String, email: String, nickname: String)
  private[controllers] val addMemberForm = Form(
    mapping(
      "role" -> default(nonEmptyText, MembershipRole.Member.toString),
      "email" -> text,
      "nickname" -> text
    )(AddMemberData.apply)(AddMemberData.unapply)
  )

}
