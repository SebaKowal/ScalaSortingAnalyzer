package ui.tabs

import model.{AlgorithmDetail, AlgorithmType}
import scalafx.geometry.Insets
import scalafx.scene.control.*
import scalafx.scene.layout.*

class DescriptionTab(selectedAlgo: () => AlgorithmType):

  private def codeBlock(code: String, lang: String): VBox =
    val header = new HBox:
      style = "-fx-background-color: #2a2d45; -fx-padding: 6 12;"
      children = Seq(
        new Label(lang):
          style = "-fx-text-fill: #7986cb; -fx-font-size: 10px; -fx-font-family: 'Consolas';"
      )
    val area = new TextArea(code):
      editable = false
      wrapText = false
      prefRowCount = code.count(_ == '\n') + 2
      style = """-fx-background-color: #12142b;
                |-fx-text-fill: #e0e0ff;
                |-fx-font-family: 'Consolas', 'Courier New', monospace;
                |-fx-font-size: 12px;
                |-fx-border-color: transparent;
                |-fx-focus-color: transparent;
                |-fx-faint-focus-color: transparent;""".stripMargin
    new VBox(0, header, area):
      style = "-fx-border-color: #2a2d45; -fx-border-radius: 6; -fx-background-radius: 6;"

  private def badge(text: String, color: String): Label = new Label(text):
    style = s"-fx-background-color: ${color}22; -fx-text-fill: $color; " +
      s"-fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 11px;"

  private def sectionTitle(text: String): Label = new Label(text):
    style = "-fx-text-fill: #7986cb; -fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 8 0 4 0;"

  private def bodyText(text: String): Label = new Label(text):
    wrapText = true
    style = "-fx-text-fill: #b0b8d8; -fx-font-size: 12px; -fx-line-spacing: 2;"

  def build(): ScrollPane =
    val algoType = selectedAlgo()
    val detailOpt = AlgorithmDetail.all.get(algoType)

    detailOpt match
      case None =>
        val sp = new ScrollPane
        sp.style = "-fx-background: #0f111a;"
        sp

      case Some(detail) =>
        val algo = detail.algorithmType

        val complexityRow = new HBox(8):
          padding = Insets(4, 0, 8, 0)
          children = Seq(
            badge(s"Best: ${algo.bestCase}",   "#00e676"),
            badge(s"Avg: ${algo.avgCase}",     "#ff9800"),
            badge(s"Worst: ${algo.worstCase}", "#ff1744"),
            badge(s"Space: ${algo.space}",     "#7986cb")
          )

        val (pros, cons) = detail.prosAndCons

        val prosBox = new VBox(4):
          children = pros.map(p => new Label(s"✓  $p"):
            style = "-fx-text-fill: #00e676; -fx-font-size: 11px;")

        val consBox = new VBox(4):
          children = cons.map(c => new Label(s"✗  $c"):
            style = "-fx-text-fill: #ff6b6b; -fx-font-size: 11px;")

        val prosConsRow = new HBox(24, prosBox, consBox):
          padding = Insets(0, 0, 8, 0)

        val innerContent = new VBox(8):
          padding = Insets(16)
          style = "-fx-background-color: #0f111a;"
          children = Seq(
            sectionTitle("Complexity"),
            complexityRow,
            sectionTitle("How it works"),
            bodyText(detail.timeComplexityNotes),
            sectionTitle("Space"),
            bodyText(detail.spaceNotes),
            sectionTitle("Pros & Cons"),
            prosConsRow,
            sectionTitle("Pseudocode"),
            codeBlock(detail.pseudocode, "PSEUDOCODE"),
            sectionTitle("Scala Implementation"),
            codeBlock(detail.scalaCode, "SCALA")
          )

        val sp = new ScrollPane
        sp.content = innerContent
        sp.fitToWidth = true
        sp.style = "-fx-background: #0f111a; -fx-background-color: #0f111a; -fx-border-color: transparent;"
        sp