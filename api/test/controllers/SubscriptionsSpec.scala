package controllers

import io.apibuilder.apidoc.api.v0.models.{Publication, Subscription, SubscriptionForm}
import io.apibuilder.apidoc.api.v0.errors.{ErrorsResponse, FailedRequest, UnitResponse}
import java.util.UUID

import play.api.test._
import play.api.test.Helpers._

class SubscriptionsSpec extends BaseSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  lazy val org = createOrganization()

  "POST /subscriptions" in new WithServer {
    val user = createUser()
    val subscription = createSubscription(
      SubscriptionForm(
        organizationKey = org.key,
        userGuid = user.guid,
        publication = Publication.MembershipRequestsCreate
      )
    )

    subscription.organization.key must be(org.key)
    subscription.user.guid must be(user.guid)
    subscription.publication must be(Publication.MembershipRequestsCreate)
  }

  "POST /subscriptions handles user already subscribed" in new WithServer {
    val user = createUser()
    val form = createSubscriptionForm(org, user)
    val subscription = createSubscription(form)

    intercept[ErrorsResponse] {
      createSubscription(form)
    }.errors.map(_.message) must be(Seq("User is already subscribed to this publication for this organization"))
  }

  "POST /subscriptions allows user to subscribe to a different organization" in new WithServer {
    val user = createUser()
    val form = createSubscriptionForm(org, user)
    val subscription1 = createSubscription(form)

    subscription1.organization.key must be(org.key)
    subscription1.user.guid must be(user.guid)
    subscription1.publication must be(Publication.MembershipRequestsCreate)

    val org2 = createOrganization()
    val subscription2 = createSubscription(form.copy(organizationKey = org2.key))
    subscription2.organization.key must be(org2.key)
    subscription2.user.guid must be(user.guid)
    subscription2.publication must be(Publication.MembershipRequestsCreate)
  }

  "POST /subscriptions validates org key" in new WithServer {
    val user = createUser()

    intercept[ErrorsResponse] {
      createSubscription(
        SubscriptionForm(
          organizationKey = UUID.randomUUID.toString,
          userGuid = user.guid,
          publication = Publication.MembershipRequestsCreate
        )
      )
    }.errors.map(_.message) must be(Seq("Organization not found"))
  }

  "POST /subscriptions validates user guid" in new WithServer {
    intercept[ErrorsResponse] {
      createSubscription(
        SubscriptionForm(
          organizationKey = org.key,
          userGuid = UUID.randomUUID,
          publication = Publication.MembershipRequestsCreate
        )
      )
    }.errors.map(_.message) must be(Seq("User not found"))
  }

  "POST /subscriptions validates publication" in new WithServer {
    val user = createUser()

    intercept[ErrorsResponse] {
      createSubscription(
        SubscriptionForm(
          organizationKey = org.key,
          userGuid = user.guid,
          publication = Publication(UUID.randomUUID.toString)
        )
      )
    }.errors.map(_.message) must be(Seq("Publication not found"))
  }

  "DELETE /subscriptions/:guid" in new WithServer {
    val subscription = createSubscription(createSubscriptionForm(org))
    await(client.subscriptions.deleteByGuid(subscription.guid)) must be(())
    await(client.subscriptions.deleteByGuid(subscription.guid)) must be(()) // test idempotence
    intercept[UnitResponse] {
      await(client.subscriptions.getByGuid(subscription.guid))
    }.status must be(404)

    // now recreate
    val subscription2 = createSubscription(createSubscriptionForm(org))
    await(client.subscriptions.getByGuid(subscription2.guid)) must be(subscription2)
  }


  "GET /subscriptions/:guid" in new WithServer {
    val subscription = createSubscription(createSubscriptionForm(org))
    await(client.subscriptions.getByGuid(subscription.guid)) must be(subscription)

    intercept[UnitResponse] {
      await(client.subscriptions.getByGuid(UUID.randomUUID))
    }.status must be(404)
  }

  "GET /subscriptions filters" in new WithServer {
    val user1 = createUser()
    val user2 = createUser()
    val org1 = createOrganization()
    val org2 = createOrganization()
    val subscription1 = createSubscription(
      SubscriptionForm(
        organizationKey = org1.key,
        userGuid = user1.guid,
        publication = Publication.MembershipRequestsCreate
      )
    )

    val subscription2 = createSubscription(
      SubscriptionForm(
        organizationKey = org2.key,
        userGuid = user2.guid,
        publication = Publication.ApplicationsCreate
      )
    )

    await(client.subscriptions.get(organizationKey = Some(UUID.randomUUID.toString))) must be(Seq.empty)
    await(client.subscriptions.get(organizationKey = Some(org1.key))).map(_.guid) must be(Seq(subscription1.guid))
    await(client.subscriptions.get(organizationKey = Some(org2.key))).map(_.guid) must be(Seq(subscription2.guid))

    await(client.subscriptions.get(userGuid = Some(UUID.randomUUID))) must be(Seq.empty)
    await(client.subscriptions.get(userGuid = Some(user1.guid))).map(_.guid) must be(Seq(subscription1.guid))
    await(client.subscriptions.get(userGuid = Some(user2.guid))).map(_.guid) must be(Seq(subscription2.guid))

    await(client.subscriptions.get(userGuid = Some(user1.guid), publication = Some(Publication.MembershipRequestsCreate))).map(_.guid) must be(Seq(subscription1.guid))
    await(client.subscriptions.get(userGuid = Some(user2.guid), publication = Some(Publication.ApplicationsCreate))).map(_.guid) must be(Seq(subscription2.guid))

    intercept[FailedRequest] {
      await(client.subscriptions.get(publication = Some(Publication(UUID.randomUUID.toString)))) must be(Seq.empty)
    }.responseCode must be(400)
  }

  "GET /subscriptions authorizes user" in new WithServer {
    val subscription = createSubscription(createSubscriptionForm(org))
    val randomUser = createUser()

    await(client.subscriptions.get(guid = Some(subscription.guid))).map(_.guid) must be(Seq(subscription.guid))
    await(newClient(randomUser).subscriptions.get(guid = Some(subscription.guid))).map(_.guid) must be(Nil)
  }

}
