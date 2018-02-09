package filters

import javax.inject.Inject
import akka.stream.Materializer
import play.api.http.HttpFilters
import play.api.Logger
import play.api.mvc._
import play.filters.cors.CORSFilter
import scala.concurrent.{ExecutionContext, Future}

/**
  * Taken from lib-play to avoid pulling in lib-play as a dependency
  */
class CorsWithLoggingFilter @javax.inject.Inject() (corsFilter: CORSFilter, loggingFilter: LoggingFilter) extends HttpFilters {
  def filters = Seq(corsFilter, loggingFilter)
}

class LoggingFilter @Inject() (implicit val mat: Materializer, ec: ExecutionContext) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])
           (requestHeader: RequestHeader): Future[Result] = {

    val startTime = System.currentTimeMillis
    val headerMap = requestHeader.headers.toMap

    nextFilter(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime

      val line = Seq(
        requestHeader.method,
        s"${requestHeader.host}${requestHeader.path}",
        result.header.status,
        s"${requestTime}ms",
        headerMap.getOrElse("User-Agent", Nil).mkString(","),
        headerMap.getOrElse("X-Forwarded-For", Nil).mkString(","),
        headerMap.getOrElse(
          "CF-Connecting-IP",
          headerMap.getOrElse("True-Client-IP", Nil)
        ).mkString(",")
      ).mkString(" ")

      Logger.info(line)

      result.withHeaders("Request-Time" -> requestTime.toString)
    }
  }
}
