import play.api.Application
import play.api.GlobalSettings
import play.api.mvc._

object Global extends GlobalSettings {
  override def onStart(app: Application) = {
    implicit val current = app // db stuff below needs an implicit Application
    import play.api.db._
    import anorm._
    DB.withConnection { implicit c =>
      SQL("""
      create table organizations (
        guid uuid primary key,
        name varchar not null unique
      )
      """).execute()
      SQL("""
      create table users (
        guid uuid primary key,
        email varchar not null unique,
        active boolean not null,
        profile varchar
      )
      """).execute()
      SQL("""
      create table members (
        guid uuid primary key,
        organization uuid not null references organizations,
        user uuid not null references users,
        role varchar not null
      )
      """).execute()
    }
  }
}
