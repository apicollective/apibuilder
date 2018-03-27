package lib

import io.apibuilder.api.v0.models.{SortOrder, AppSortBy}

object AppOrderHelper {
  def newOrder(currSort: Option[AppSortBy], currOrd: Option[SortOrder], heading: AppSortBy) = {
    if (currSort == Some(heading)) {
      currOrd match {
        case Some(SortOrder.Desc) => Some(SortOrder.Asc)
        case _ => Some(SortOrder.Desc)
      }
    } else {
      Some(SortOrder.Asc)
    }
  }

  def orderImage(currSort: Option[AppSortBy], currOrd: Option[SortOrder], heading: AppSortBy) = {
    if (currSort == Some(heading)) {
      currOrd match {
        case Some(SortOrder.Desc) => views.html.icon("icons/sort-descending-2x.png", "descending")
        case _ => views.html.icon("icons/sort-ascending-2x.png", "ascending")
      }
    } else {
      ""
    }
  }
}
