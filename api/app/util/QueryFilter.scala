package util

import io.flow.postgresql.Query

abstract class QueryFilter {
  def filter(q: Query): Query
}

abstract class OptionalQueryFilter[T](value: Option[T]) extends QueryFilter {
  final def filter(q: Query): Query =
    value match {
      case None => q
      case Some(v) => filter(q, v)
    }
  def filter(q: Query, value: T): Query
}
