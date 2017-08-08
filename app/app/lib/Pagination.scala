package lib

object Pagination {

  val DefaultLimit = 50

}

case class PaginatedCollection[T](page: Int, allItems: Seq[T], limit: Int = Pagination.DefaultLimit) {

  def items = allItems.take(limit)

  lazy val hasPrevious: Boolean = {
    page > 0
  }

  lazy val hasNext: Boolean = {
    allItems.length > limit
  }

  lazy val isEmpty: Boolean = {
    allItems.isEmpty
  }

}
