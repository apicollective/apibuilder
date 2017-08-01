package controllers

import io.apibuilder.api.v0.models.{Publication, SubscriptionForm}
import java.util.UUID
import play.api.test._

class SubscriptionsSpec extends PlaySpecification with MockClient {
  import scala.concurrent.ExecutionContext.Implicits.global

  private[this] lazy val org = createOrganization()

  "POST /subscriptions" in new WithServer(port=defaultPort) {
    val user = createUser()
    val subscription = await(client.subscriptions.post(
      SubscriptionForm(
        organizationKey = org.key,
        userGuid = user.guid,
        publication = Publication.MembershipRequestsCreate
      )
    ))

    subscription.organization.key must beEqualTo(org.key)
    subscription.user.guid must beEqualTo(user.guid)
    subscription.publication must beEqualTo(Publication.MembershipRequestsCreate)
  }

  "POST /subscriptions handles user already subscribed" in new WithServer(port=defaultPort) {
    val user = createUser()
    val form = createSubscriptionForm(org, user)
    val subscription = await(client.subscriptions.post(form))

    expectErrors {
      client.subscriptions.post(form)
    }
  }

  "POST /subscriptions allows user to subscribe to a different organization" in new WithServer(port=defaultPort) {
    val user = createUser()
    val form = createSubscriptionForm(org, user)
    val subscription1 = await(client.subscriptions.post(form))

    subscription1.organization.key must beEqualTo(org.key)
    subscription1.user.guid must beEqualTo(user.guid)
    subscription1.publication must beEqualTo(Publication.MembershipRequestsCreate)

    val org2 = createOrganization()
    val subscription2 = await(client.subscriptions.post(form.copy(organizationKey = org2.key)))
    subscription2.organization.key must beEqualTo(org2.key)
    subscription2.user.guid must beEqualTo(user.guid)
    subscription2.publication must beEqualTo(Publication.MembershipRequestsCreate)
  }

  "POST /subscriptions validates org key" in new WithServer(port=defaultPort) {
    val user = createUser()

    expectErrors {
      client.subscriptions.post(
        SubscriptionForm(
          organizationKey = UUID.randomUUID.toString,
          userGuid = user.guid,
          publication = Publication.MembershipRequestsCreate
        )
      )
    }.errors.map(_.message) must beEqualTo(Seq("Organization not found"))
  }

  "POST /subscriptions validates user guid" in new WithServer(port=defaultPort) {
    expectErrors {
      client.subscriptions.post(
        SubscriptionForm(
          organizationKey = org.key,
          userGuid = UUID.randomUUID,
          publication = Publication.MembershipRequestsCreate
        )
      )
    }.errors.map(_.message) must beEqualTo(Seq("User not found"))
  }

  "POST /subscriptions validates publication" in new WithServer(port=defaultPort) {
    val user = createUser()

    expectErrors {
      client.subscriptions.post(
        SubscriptionForm(
          organizationKey = org.key,
          userGuid = user.guid,
          publication = Publication(UUID.randomUUID.toString)
        )
      )
    }.errors.map(_.message) must beEqualTo(Seq("Publication not found"))
  }

  "DELETE /subscriptions/:guid" in new WithServer(port=defaultPort) {
    val subscription = await(client.subscriptions.post(createSubscriptionForm(org)))
    expectStatus(204) {
      client.subscriptions.deleteByGuid(subscription.guid)
    }

    await(client.subscriptions.deleteByGuid(subscription.guid)) must beEqualTo(()) // test idempotence
    expectNotFound {
      client.subscriptions.getByGuid(subscription.guid)
    }

    // now recreate
    val subscription2 = await(client.subscriptions.post(createSubscriptionForm(org)))
    await(client.subscriptions.getByGuid(subscription2.guid)) must beEqualTo(subscription2)
  }


  "GET /subscriptions/:guid" in new WithServer(port=defaultPort) {
    val subscription = await(client.subscriptions.post(createSubscriptionForm(org)))
    await(client.subscriptions.getByGuid(subscription.guid)) must beEqualTo(subscription)

    expectNotFound {
      client.subscriptions.getByGuid(UUID.randomUUID)
    }
  }

  "GET /subscriptions filters" in new WithServer(port=defaultPort) {
    val user1 = createUser()
    val user2 = createUser()
    val org1 = createOrganization()
    val org2 = createOrganization()
    val subscription1 = await(client.subscriptions.post(
      SubscriptionForm(
        organizationKey = org1.key,
        userGuid = user1.guid,
        publication = Publication.MembershipRequestsCreate
      )
    ))

    val subscription2 = await(client.subscriptions.post(
      SubscriptionForm(
        organizationKey = org2.key,
        userGuid = user2.guid,
        publication = Publication.ApplicationsCreate
      )
    ))

    await(client.subscriptions.get(organizationKey = Some(UUID.randomUUID.toString))) must beEqualTo(Nil)
    await(client.subscriptions.get(organizationKey = Some(org1.key))).map(_.guid) must beEqualTo(Seq(subscription1.guid))
    await(client.subscriptions.get(organizationKey = Some(org2.key))).map(_.guid) must beEqualTo(Seq(subscription2.guid))

    await(client.subscriptions.get(userGuid = Some(UUID.randomUUID))) must beEqualTo(Nil)
    await(client.subscriptions.get(userGuid = Some(user1.guid))).map(_.guid) must beEqualTo(Seq(subscription1.guid))
    await(client.subscriptions.get(userGuid = Some(user2.guid))).map(_.guid) must beEqualTo(Seq(subscription2.guid))

    await(client.subscriptions.get(userGuid = Some(user1.guid), publication = Some(Publication.MembershipRequestsCreate))).map(_.guid) must beEqualTo(Seq(subscription1.guid))
    await(client.subscriptions.get(userGuid = Some(user2.guid), publication = Some(Publication.ApplicationsCreate))).map(_.guid) must beEqualTo(Seq(subscription2.guid))

    expectStatus(400) {
      client.subscriptions.get(publication = Some(Publication(UUID.randomUUID.toString)))
    }
  }

  "GET /subscriptions authorizes user" in new WithServer(port=defaultPort) {
    val subscription = await(client.subscriptions.post(createSubscriptionForm(org)))
    val randomUser = createUser()

    await(client.subscriptions.get(guid = Some(subscription.guid))).map(_.guid) must beEqualTo(Seq(subscription.guid))
    await(newClient(randomUser).subscriptions.get(guid = Some(subscription.guid))).map(_.guid) must beEqualTo(Nil)
  }

}
