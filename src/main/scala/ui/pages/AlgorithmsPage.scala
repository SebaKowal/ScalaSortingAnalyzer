package ui.pages

import model.{AlgorithmDetail, AlgorithmInfo, AlgorithmType}
import ui.Theme
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.*
import scalafx.scene.layout.*
import scalafx.beans.property.ObjectProperty
import scalafx.Includes.*

object AlgorithmsPage:

  def build(): HBox =
    val selectedAlgo = ObjectProperty[AlgorithmType](AlgorithmType.BubbleSort)

    // ── Sidebar ───────────────────────────────────────────────
    val sidebar = new VBox(0)
    sidebar.prefWidth = 220
    sidebar.minWidth  = 220
    sidebar.maxWidth  = 220
    sidebar.style = s"-fx-background-color: ${Theme.BgBase}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 1 0 0;"

    val sideHdr = new Label("ALGORITHMS")
    sideHdr.style   = Theme.titleStyle(9)
    sideHdr.padding = Insets(16, 14, 12, 14)

    val divider = new Region
    divider.prefHeight = 1
    divider.maxWidth   = Double.MaxValue
    divider.style = s"-fx-background-color: ${Theme.BgBorder};"
    sidebar.children.addAll(sideHdr.delegate, divider)

    AlgorithmType.values.foreach { algo =>
      val nameL = new Label(algo.label)
      nameL.style = Theme.labelStyle(11, Theme.TextNormal)
      val complexL = new Label(s"avg ${algo.avgCase}")
      complexL.style = Theme.labelStyle(9, Theme.TextDim)
      val itemBox = new VBox(2)
      itemBox.padding  = Insets(10, 14, 10, 14)
      itemBox.maxWidth = Double.MaxValue
      itemBox.children.addAll(nameL.delegate, complexL.delegate)
      itemBox.style = "-fx-cursor: hand;"

      def setActive(active: Boolean): Unit =
        if active then
          itemBox.style = s"-fx-background-color: ${Theme.BgRaised}; -fx-cursor: hand; " +
            s"-fx-border-color: ${Theme.AccentPrimary}; -fx-border-width: 0 0 0 3;"
          nameL.style   = Theme.labelStyle(11, Theme.AccentPrimary)
        else
          itemBox.style = "-fx-cursor: hand;"
          nameL.style   = Theme.labelStyle(11, Theme.TextNormal)

      setActive(selectedAlgo.value == algo)
      selectedAlgo.onChange { (_, _, a) => setActive(a == algo) }
      itemBox.delegate.setOnMouseClicked(_ => selectedAlgo.value = algo)
      itemBox.delegate.setOnMouseEntered(_ =>
        if selectedAlgo.value != algo then
          itemBox.style = s"-fx-background-color: ${Theme.BgRaised}; -fx-cursor: hand;"
      )
      itemBox.delegate.setOnMouseExited(_ => setActive(selectedAlgo.value == algo))
      sidebar.children.add(itemBox.delegate)
    }

    // ── Detail pane ───────────────────────────────────────────
    val detailInner = new VBox(0)
    detailInner.padding = Insets(28, 32, 28, 32)
    detailInner.style   = s"-fx-background-color: ${Theme.BgDeep};"

    def spacer(h: Int = 8): Region =
      val r = new Region; r.prefHeight = h; r

    def sectionHdr(text: String): Label =
      val l = new Label(text)
      l.style = Theme.titleStyle(9)
      VBox.setMargin(l, Insets(16, 0, 6, 0))
      l

    def badge(key: String, value: String, color: String): VBox =
      val k = new Label(key)
      k.style = Theme.labelStyle(8, Theme.TextDim)
      val v = new Label(value)
      v.style = s"-fx-text-fill: $color; -fx-font-size: 13px; " +
        s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"
      val box = new VBox(2)
      box.padding = Insets(8, 12, 8, 12)
      box.style   = Theme.cardStyle
      box.children.addAll(k.delegate, v.delegate)
      box

    // Code block as a Label — wraps naturally, no scrollbars, fully dark
    def codeBlock(code: String): VBox =
      val lbl = new Label(code)
      lbl.style =
        s"-fx-text-fill: ${Theme.TextBright}; " +
          s"-fx-font-family: 'Consolas', 'Courier New', monospace; " +
          s"-fx-font-size: 11px; " +
          s"-fx-line-spacing: 2;"
      lbl.wrapText  = true
      lbl.maxWidth  = Double.MaxValue
      lbl.padding   = Insets(14, 16, 14, 16)

      val outer = new VBox(0)
      outer.style =
        s"-fx-background-color: ${Theme.BgBase}; " +
          s"-fx-border-color: ${Theme.BgBorder}; " +
          s"-fx-border-radius: 4; " +
          s"-fx-background-radius: 4;"
      outer.maxWidth = Double.MaxValue
      outer.children.add(lbl.delegate)
      outer

    def refresh(algo: AlgorithmType): Unit =
      detailInner.children.clear()

      val titleL = new Label(algo.label.toUpperCase)
      titleL.style = s"-fx-text-fill: ${Theme.TextBright}; -fx-font-size: 22px; " +
        s"-fx-font-weight: bold; -fx-font-family: 'Consolas', monospace;"

      val badgeRow = new HBox(8)
      badgeRow.padding = Insets(8, 0, 0, 0)
      Seq(
        ("BEST",  algo.bestCase,  Theme.AccentSuccess),
        ("AVG",   algo.avgCase,   Theme.AccentSecondary),
        ("WORST", algo.worstCase, Theme.AccentDanger),
        ("SPACE", algo.space,     Theme.AccentPrimary)
      ).foreach { (k, v, c) => badgeRow.children.add(badge(k, v, c).delegate) }

      detailInner.children.addAll(titleL.delegate, badgeRow.delegate)

      AlgorithmInfo.all.get(algo).foreach { info =>
        detailInner.children.addAll(
          sectionHdr("DESCRIPTION").delegate,
          new Label(info.description):
            style    = Theme.labelStyle(12, Theme.TextNormal)
            wrapText = true
            maxWidth = Double.MaxValue
        )
      }

      AlgorithmDetail.all.get(algo).foreach { detail =>
        detailInner.children.addAll(
          sectionHdr("TIME COMPLEXITY NOTES").delegate,
          new Label(detail.timeComplexityNotes):
            style    = Theme.labelStyle(12, Theme.TextNormal)
            wrapText = true
            maxWidth = Double.MaxValue
        )
        detailInner.children.addAll(
          sectionHdr("SPACE COMPLEXITY").delegate,
          new Label(detail.spaceNotes):
            style    = Theme.labelStyle(12, Theme.TextNormal)
            wrapText = true
            maxWidth = Double.MaxValue
        )

        val (pros, cons) = detail.prosAndCons
        val prosVBox = new VBox(4)
        pros.foreach { p =>
          val l = new Label(s"+ $p")
          l.style = Theme.labelStyle(11, Theme.TextNormal)
          l.wrapText = true; l.maxWidth = 300
          prosVBox.children.add(l.delegate)
        }
        val consVBox = new VBox(4)
        cons.foreach { c =>
          val l = new Label(s"- $c")
          l.style = Theme.labelStyle(11, Theme.TextNormal)
          l.wrapText = true; l.maxWidth = 300
          consVBox.children.add(l.delegate)
        }
        val pcRow = new HBox(32, prosVBox, consVBox)
        pcRow.padding = Insets(0, 0, 8, 0)
        detailInner.children.addAll(sectionHdr("PROS & CONS").delegate, pcRow.delegate)

        // ── Code tabs ─────────────────────────────────────────
        val codeTabs = new TabPane
        codeTabs.minHeight = 0
        codeTabs.style =
          s"""-fx-background-color: ${Theme.BgDeep};
             |-fx-tab-min-height: 32px;
             |-fx-tab-max-height: 32px;
             |-fx-open-tab-animation: NONE;
             |-fx-close-tab-animation: NONE;""".stripMargin
        codeTabs.delegate.getStylesheets.add(Theme.tabPaneStylesheet)

        def makeTab(title: String, node: scalafx.scene.Node): Tab =
          val t = new Tab
          t.text = title; t.closable = false; t.content = node
          t

        val pseudoWrap = new VBox(0)
        pseudoWrap.padding = Insets(12)
        pseudoWrap.style   = s"-fx-background-color: ${Theme.BgDeep};"
        pseudoWrap.children.add(codeBlock(detail.pseudocode).delegate)

        val scalaWrap = new VBox(0)
        scalaWrap.padding = Insets(12)
        scalaWrap.style   = s"-fx-background-color: ${Theme.BgDeep};"
        scalaWrap.children.add(codeBlock(detail.scalaCode).delegate)

        codeTabs.tabs.addAll(
          makeTab("  PSEUDOCODE  ", pseudoWrap).delegate,
          makeTab("  SCALA CODE  ", scalaWrap).delegate
        )

        detailInner.children.addAll(
          sectionHdr("IMPLEMENTATION").delegate,
          codeTabs.delegate
        )
      }

    refresh(selectedAlgo.value)
    selectedAlgo.onChange { (_, _, algo) => refresh(algo) }

    val detailScroll = new ScrollPane
    detailScroll.content    = detailInner
    detailScroll.fitToWidth = true
    detailScroll.style      = Theme.scrollPaneStyle
    HBox.setHgrow(detailScroll, Priority.Always)
    VBox.setVgrow(detailScroll, Priority.Always)

    val page = new HBox
    page.children.addAll(sidebar.delegate, detailScroll.delegate)
    VBox.setVgrow(detailScroll, Priority.Always)
    page