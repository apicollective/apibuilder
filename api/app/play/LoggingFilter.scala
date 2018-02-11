package io.apicollective.play

import akka.stream.Materializer
import play.api.Logger
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import play.api.http.HttpFilters

/**
  * Add this to your base.conf:
  *    play.http.filters=io.apicollective.play.LoggingFilter
  **/
class LoggingFilter @javax.inject.Inject() (loggingFilter: ApibuilderLoggingFilter) extends HttpFilters {
  def filters = Seq(loggingFilter)
}

class ApibuilderLoggingFilter @javax.inject.Inject() (
  implicit ec: ExecutionContext,
  m: Materializer
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
        headerMap.getOrElse("User-Agent", Nil).mkString(",")
      ).mkString(" ")

      Logger.info(line)
      result
    }
  }

  override implicit def mat: Materializer = m
}
