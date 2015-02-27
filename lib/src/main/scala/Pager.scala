package lib

object Pager {

  /**
    * Iterator that takes two functions:
    *   pagerFunction: Method to return a page of results
    *   perObjectFunction: Function to call on each element
    * 
    * Example:
    * Pager.eachPage[Subscription] { offset =>
    *   SubscriptionsDao.findAll(
    *     Authorization.All,
    *     organization = Some(organization),
    *     publication = Some(publication),
    *     limit = 100,
    *     offset = offset
    *   )
    * } { subscription =>
    *   println(subscription)
    * }
    */
  def eachPage[T](
    pagerFunction: Int => Seq[T]
  )(
    perObjectFunction: T => Unit
  ) {
    var offset = 0
    var haveMore = true

    while (haveMore) {
      val objects = pagerFunction(offset)
      haveMore = !objects.isEmpty
      offset += objects.size
      objects.foreach { perObjectFunction(_) }
    }
  }

}
