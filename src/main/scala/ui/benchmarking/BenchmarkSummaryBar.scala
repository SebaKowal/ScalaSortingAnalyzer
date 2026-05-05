package ui.benchmarking

import benchmark.full.FullResult
import benchmark.pure.PureResult
import ui.utils.Theme
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.control.Label
import scalafx.scene.layout.*
import scalafx.Includes.*

/** Reactive summary bar below the table.
 *  Works with both PureResult and FullResult since FullResult delegates to PureResult. */
class BenchmarkSummaryBar(results: ObservableBuffer[AnyRef]):

  val node: VBox = new VBox(6):
    padding   = Insets(10, 14, 10, 14)
    minHeight = 60
    style     = s"-fx-background-color: ${Theme.BgBase}; " +
      s"-fx-border-color: ${Theme.BgBorder}; -fx-border-width: 1 0 0 0;"

  results.onChange { (_, _) => Platform.runLater { rebuild() } }

  private def asPure(r: AnyRef): PureResult = r match
    case p: PureResult => p
    case f: FullResult => f.pure

  private def chip(text: String, color: String): Label =
    val l = new Label(text)
    l.style = s"-fx-background-color: ${color}22; -fx-text-fill: $color; " +
      s"-fx-padding: 2 7 2 7; -fx-background-radius: 3; " +
      s"-fx-font-family: 'Consolas', monospace; -fx-font-size: 10px;"
    l

  private def hdr(text: String): Label =
    val l = new Label(text); l.style = Theme.titleStyle(9); l

  private def rebuild(): Unit =
    node.children.clear()
    val valid  = results.toSeq.map(asPure).filter(!_.hasFailure)
    val failed = results.toSeq.map(asPure).filter(_.hasFailure)
    if valid.isEmpty && failed.isEmpty then return

    if failed.nonEmpty then
      val row = new HBox(8); row.alignment = Pos.CenterLeft
      val h = new Label("FAILURES"); h.style = Theme.titleStyle(9, Theme.AccentDanger)
      row.children.add(h.delegate)
      failed.groupBy(_.algoName).foreach { (algo, _) =>
        row.children.add(chip(s"✗ $algo", Theme.AccentDanger).delegate)
      }
      node.children.add(row.delegate)

    if valid.nonEmpty then
      val fastest    = valid.minBy(_.meanNs)
      val slowest    = valid.maxBy(_.meanNs)
      val leastComps = valid.minBy(_.comparisons)

      val row = new HBox(10); row.alignment = Pos.CenterLeft
      row.children.addAll(
        hdr("SUMMARY").delegate,
        chip(s"Fastest: ${fastest.algoName}/${fastest.pattern} (${fastest.timeMsStr})", Theme.AccentSuccess).delegate,
        chip(s"Slowest: ${slowest.algoName}/${slowest.pattern} (${slowest.timeMsStr})", Theme.AccentDanger).delegate,
        chip(s"Fewest cmps: ${leastComps.algoName} (${f"${leastComps.comparisons}%,d"})", Theme.AccentPrimary).delegate
      )
      node.children.add(row.delegate)

      // Full-mode extra summary — only if FullResults are present
      val fullItems = results.toSeq.collect { case f: FullResult => f }
      if fullItems.nonEmpty then
        val lowestGc  = fullItems.minBy(_.gcCollections)
        val highestGc = fullItems.maxBy(_.gcCollections)
        val gcRow = new HBox(10); gcRow.alignment = Pos.CenterLeft
        gcRow.children.addAll(
          hdr("SYSTEM").delegate,
          chip(s"Lowest GC: ${lowestGc.algoName} (${lowestGc.gcCollections})", Theme.AccentSuccess).delegate,
          chip(s"Highest GC: ${highestGc.algoName} (${highestGc.gcCollections})", Theme.AccentDanger).delegate
        )
        node.children.add(gcRow.delegate)