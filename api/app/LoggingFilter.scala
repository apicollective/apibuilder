package io.apicollective

import play.api.Logger
import scala.concurrent.{ExecutionContext, Future}
import play.api.http.HttpFilters

/**
  * To use in any Flow app depending on lib-play:
  *
  * (1) Add this to your base.conf:
  *    play.http.filters=io.apicollective.LoggingFilter
  *
  **/

class LoggingFilter @javax.inject.Inject() (loggingFilter: cLoggingFilter) extends HttpFilters {
  def filters = Seq(loggingFilter)
}

class ApibuilderLoggingFilter @javax.inject.Inject() (
  implicit ec: ExecutionContext,
) extends Filter {
  def apply(f: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    val startTime = System.currentTimeMillis
    f(requestHeader).map { result =>
      val endTime = System.currentTimeMillis
      val requestTime = endTime - startTime
      val headerMap = requestHeader.headers.toMap
      val line = Seq(
        requestHeader.method,
        s"${requestHeader.host}${requestHeader.uri}",
        result.header.status,
        s"${requestTime}ms",
        headerMap.getOrElse("User-Agent", Nil).mkString(","),
        headerMap.getOrElse("X-Forwarded-For", Nil).mkString(","),
        headerMap.getOrElse("CF-Connecting-IP", Nil).mkString(",")
      ).mkString(" ")

      Logger.info(line)

      result
    }
  }
}
