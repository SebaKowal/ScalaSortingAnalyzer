package ui

import app.AppState
import benchmark.BenchmarkState
import ui.pages.{AlgorithmsPage, BenchmarkAnalysisPage, BenchmarkPage, VisualizerPage}
import scalafx.scene.layout.*
import scalafx.Includes.*

object MainView:
  def apply(): BorderPane =
    val state  = AppState.instance
    val navbar = NavBar.build()

    // Build all pages once
    val vizPage      = VisualizerPage.build(state)
    val algoPage     = AlgorithmsPage.build()
    val benchPage    = BenchmarkPage.build()
    val analysisPage = BenchmarkAnalysisPage.build(BenchmarkState.results)

    // Page container — swaps content on nav change
    val pageArea = new StackPane
    pageArea.style = s"-fx-background-color: ${Theme.BgDeep};"
    VBox.setVgrow(pageArea, Priority.Always)

    def showPage(page: Page): Unit =
      pageArea.children.clear()
      val node = page match
        case Page.Visualizer  => vizPage.delegate
        case Page.Algorithms  => algoPage.delegate
        case Page.Benchmark   => benchPage.delegate
        case Page.Analysis    => analysisPage.delegate
      pageArea.children.add(node)

    showPage(AppRouter.currentPage.value)
    AppRouter.currentPage.onChange { (_, _, p) => showPage(p) }

    val root = new BorderPane
    root.style  = s"-fx-background-color: ${Theme.BgDeep};"
    root.top    = navbar
    root.center = pageArea
    root
