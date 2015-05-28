package db.generators

import org.scalatest.{FunSpec, Matchers}
import java.util.UUID

class SourcesDaoSpec extends FunSpec with Matchers {

  new play.core.StaticApplication(new java.io.File("."))

  def createGeneratorSourceForm(
    uri: String = s"http://test.generator.${UUID.randomUUID}"
  ): SourceForm = {
    SourceForm(
      uri = uri
    )
  }

  it("validate") {
    SourcesDao.validate(SourceForm("http://foo.com")) should be(Nil)
    SourcesDao.validate(SourceForm("foo")).map(_.message) should be(Seq("URI[foo] must start with http://, https://, or file://"))

    val form = createGeneratorSourceForm()
    val gen = SourcesDao.create(db.Util.createdBy, form)
    SourcesDao.validate(form).map(_.message) should be(Seq(s"URI[${form.uri}] already exists"))
  }

  describe("create") {

    it("creates a source") {
      val form = createGeneratorSourceForm()
      val gen = SourcesDao.create(db.Util.createdBy, form)
      gen.uri should be(form.uri)
    }

    it("raises error on duplicate uri") {
      val form = createGeneratorSourceForm()
      SourcesDao.create(db.Util.createdBy, form)
      intercept[AssertionError] {
        SourcesDao.create(db.Util.createdBy, form)
      }
    }

  }

  it("softDelete") {
    val gen = SourcesDao.create(db.Util.createdBy, createGeneratorSourceForm())
    SourcesDao.softDelete(db.Util.createdBy, gen)
    SourcesDao.findByGuid(gen.guid) should be(None)
    SourcesDao.findAll(guid = Some(gen.guid), isDeleted = None).map(_.guid) should be(Seq(gen.guid))
    SourcesDao.findAll(guid = Some(gen.guid), isDeleted = Some(true)).map(_.guid) should be(Seq(gen.guid))
  }

  describe("findAll") {

    it("uri") {
      val form = createGeneratorSourceForm()
      val gen = SourcesDao.create(db.Util.createdBy, form)
      SourcesDao.findAll(uri = Some(form.uri)).map(_.uri) should be(Seq(form.uri))
      SourcesDao.findAll(uri = Some(form.uri + "2")) should be(Nil)
    }

  }

}
