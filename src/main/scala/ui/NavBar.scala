package ui

import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Label
import scalafx.scene.layout.*
import scalafx.Includes.*

object NavBar:
  def build(): HBox =
    val bar = new HBox
    bar.alignment  = Pos.CenterLeft
    bar.prefHeight = 48
    bar.style = s"-fx-background-color: ${Theme.BgDeep}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 0 1 0;"

    val brandDot = new javafx.scene.shape.Rectangle(3, 18)
    brandDot.setFill(javafx.scene.paint.Color.web(Theme.AccentPrimary))

    val brand = new Label("SORT.VIZ")
    brand.style = s"-fx-text-fill: ${Theme.TextBright}; -fx-font-size: 15px; " +
      s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace; " +
      s"-fx-padding: 0 0 0 10;"

    val brandBox = new HBox
    brandBox.alignment = Pos.Center
    brandBox.padding   = Insets(0, 32, 0, 20)
    brandBox.children.addAll(brandDot, brand.delegate)

    def navItem(label: String, page: Page): VBox =
      val lbl = new Label(label)
      lbl.style = Theme.labelStyle(11, Theme.TextDim)
      lbl.padding = Insets(0, 4, 0, 4)

      val indicator = new Region
      indicator.prefHeight = 2
      indicator.maxWidth   = Double.MaxValue
      indicator.style = "-fx-background-color: transparent;"

      val item = new VBox(0)
      item.alignment = Pos.Center
      item.padding   = Insets(0, 16, 0, 16)
      item.prefHeight = 48
      item.children.addAll(lbl.delegate, indicator)
      item.style = "-fx-cursor: hand;"

      def setActive(active: Boolean): Unit =
        if active then
          lbl.style       = Theme.labelStyle(11, Theme.AccentPrimary)
          indicator.style = s"-fx-background-color: ${Theme.AccentPrimary};"
          item.style      = s"-fx-cursor: hand; -fx-background-color: ${Theme.BgBase};"
        else
          lbl.style       = Theme.labelStyle(11, Theme.TextDim)
          indicator.style = "-fx-background-color: transparent;"
          item.style      = "-fx-cursor: hand; -fx-background-color: transparent;"

      setActive(AppRouter.currentPage.value == page)
      AppRouter.currentPage.onChange { (_, _, p) => setActive(p == page) }

      item.delegate.setOnMouseClicked(_ => AppRouter.go(page))
      item.delegate.setOnMouseEntered(_ =>
        if AppRouter.currentPage.value != page then
          item.style = s"-fx-cursor: hand; -fx-background-color: ${Theme.BgRaised};"
      )
      item.delegate.setOnMouseExited(_ =>
        if AppRouter.currentPage.value != page then
          item.style = "-fx-cursor: hand; -fx-background-color: transparent;"
        else
          item.style = s"-fx-cursor: hand; -fx-background-color: ${Theme.BgBase};"
      )
      item

    val vizItem   = navItem("VISUALIZER", Page.Visualizer)
    val algoItem  = navItem("ALGORITHMS", Page.Algorithms)
    val benchItem = navItem("BENCHMARK",  Page.Benchmark)
    val analysisItem = navItem("ANALYSIS", Page.Analysis)

    val spacer = new Region
    HBox.setHgrow(spacer, Priority.Always)

    val versionLbl = new Label("v1.0.0")
    versionLbl.style = s"-fx-text-fill: ${Theme.TextDim}; -fx-font-size: 9px; " +
      s"-fx-font-family: 'Consolas', monospace;"
    versionLbl.padding = Insets(0, 20, 0, 0)

    bar.children.addAll(
      brandBox.delegate,
      vizItem.delegate,
      algoItem.delegate,
      benchItem.delegate,
      analysisItem.delegate,
      spacer,
      versionLbl.delegate
    )
    bar