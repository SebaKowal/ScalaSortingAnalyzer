package app

import scalafx.beans.property.ObjectProperty

enum Page:
  case Visualizer, Algorithms

object AppRouter:
  val currentPage: ObjectProperty[Page] = ObjectProperty(Page.Visualizer)
  def go(p: Page): Unit = currentPage.value = p