package ui

import app.{AppRouter, AppState, Page}
import scalafx.scene.layout.*
import ui.algorithms.AlgorithmsPage
import ui.visualizer.VisualizerPage
import ui.utils.{NavBar, Theme}

object MainView:
  def apply(): BorderPane =
    val state = AppState.instance
    val navbar = NavBar.build()

    val vizPage = VisualizerPage.build(state)
    val algoPage = AlgorithmsPage.build()

    val pageArea = new StackPane
    pageArea.style = s"-fx-background-color: ${Theme.BgDeep};"
    VBox.setVgrow(pageArea, Priority.Always)

    def showPage(page: Page): Unit =
      pageArea.children.clear()
      val node = page match
        case Page.Visualizer => vizPage.delegate
        case Page.Algorithms => algoPage.delegate

      pageArea.children.add(node)

    showPage(AppRouter.currentPage.value)
    AppRouter.currentPage.onChange { (_, _, p) => showPage(p) }

    val root = new BorderPane
    root.style = s"-fx-background-color: ${Theme.BgDeep};"
    root.top = navbar
    root.center = pageArea
    root
