package models

import org.scalatest.FlatSpec
import org.junit.Assert._
import io.Source

class ServiceSpec extends FlatSpec {

  behavior of "A Service Like Iris Hub"

  it should "have a name" in {
    assertEquals("Iris Hub", IrisHubService.service.name)
  }

  it should "have a description" in {
    assertFalse(IrisHubService.service.description.isEmpty)
  }

  it should "parse baseUrl" in {
    assertEquals("http://svc-iris-hub/svc-iris-hub/1.0", IrisHubService.service.fullUrl(""))
  }

  it should "have a resource named vendor" in {
    IrisHubService.service.resource("vendor").getOrElse {
      fail("No vendor resource")
    }
  }

  behavior of "the Vendor resource"

  it should "have a service path" in {
    assertEquals("/vendors", IrisHubService.service.url(IrisHubService.vendor, IrisHubService.get))
  }


  it should "have a path" in {
    assertEquals("/vendors",
                 IrisHubService.vendor.path)
  }

  it should "have a description" in {
    assertEquals(Some("A vendor is one of the main concepts in IRIS HUB. All activity is tied to a vendor."),
                 IrisHubService.vendor.description)
  }

  it should "have 3 fields - guid, name, key" in {
    assertEquals("guid name key",
                 IrisHubService.vendor.fields.map(_.name).mkString(" "))
  }

  behavior of "the Guid field"

  it should "parse correctly" in {
    val vendorGuid = IrisHubService.vendor.field("guid").get
    assertEquals("guid", vendorGuid.name)
    assertEquals("string", vendorGuid.dataType)
    assertEquals(Some("Uniquely identifies this vendor"), vendorGuid.description)
    assertTrue(vendorGuid.required)
    assertEquals(None, vendorGuid.default)
    assertEquals(None, vendorGuid.format)
    assertEquals(None, vendorGuid.references)
    assertEquals(None, vendorGuid.default)
    assertEquals(None, vendorGuid.minimum)
    assertEquals(None, vendorGuid.maximum)
  }

  behavior of "vendor operations"

  it should "have a GET method" in {
    val op = IrisHubService.vendor.operation("GET").getOrElse {
      fail("GET operation not defined for vendor")
    }
  }

  it should "have a description" in {
    assertEquals(Some("Search all vendors. Results are always paginated."), IrisHubService.get.description)
  }

  it should "have 5 parameters" in {
    assertEquals(5, IrisHubService.get.parameters.size)

    assertEquals(IrisHubService.get.parameters(0),
                 Field(name = "guid",
                       description = Some("Finds the vendor with this guid. Exact match"),
                       dataType = "string",
                       required = false))

    assertEquals(IrisHubService.get.parameters(1),
                 Field(name = "name",
                       description = Some("Finds the vendor with this name. Exact match"),
                       dataType = "string",
                       required = false))

    assertEquals(IrisHubService.get.parameters(2),
                 Field(name = "key",
                       description = Some("Finds the vendor with this key. Exact match"),
                       dataType = "string",
                       required = false))

    assertEquals(IrisHubService.get.parameters(3),
                 Field(name = "limit",
                       description = Some("The number of records to return"),
                       dataType = "integer",
                       required = true,
                       default = Some("50"),
                       minimum = Some(0),
                       maximum = Some(50)))

    assertEquals(IrisHubService.get.parameters(4),
                 Field(name = "offset",
                       description = Some("Used to paginate. First page of results is 0."),
                       dataType = "integer",
                       required = true,
                       default = Some("0"),
                       minimum = Some(0)))
  }

}
