package ui.benchmarking

import benchmark.full.FullResult
import benchmark.pure.PureResult
import ui.utils.Theme
import scalafx.application.Platform
import scalafx.collections.ObservableBuffer
import scalafx.scene.control.{TableColumn, TableView, TableCell, TableRow}
import scalafx.scene.layout.{Priority, VBox}
import scalafx.beans.property.StringProperty
import scalafx.Includes.*

class BenchmarkResultsTable(
                             results:    ObservableBuffer[AnyRef],
                             controller: BenchmarkController
                           ):
  private var currentRanks: Map[(String, String, Int), Int] = Map.empty
  private var currentMode: BenchmarkMode = BenchmarkMode.Pure

  // ── Column factory ────────────────────────────────────────
  private def col(
                   title:   String,
                   w:       Double,
                   fn:      AnyRef => String,
                   colorFn: AnyRef => String = _ => Theme.TextNormal
                 ): TableColumn[AnyRef, String] =
    val c = new TableColumn[AnyRef, String](title)
    c.prefWidth        = w
    c.cellValueFactory = cdf => StringProperty(fn(cdf.value))

    // Poprawiony CellFactory
    c.delegate.setCellFactory { _ =>
      new javafx.scene.control.TableCell[AnyRef, String]:
        override def updateItem(item: String, empty: Boolean): Unit =
          super.updateItem(item, empty)

          if empty || item == null then
            setText(null)
            setStyle("")
          else
            setText(item)
            // Używamy delegate, aby dobrać się do metody JavaFX lub bezpośrednio getTableRow
            val row = getTableRow()
            val color = if row != null && row.getItem != null then
              colorFn(row.getItem)
            else
              Theme.TextNormal

            setStyle(s"-fx-text-fill: $color; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-alignment: CENTER;")
    }
    c

  // ── Helpers ───────────────────────────────────────────────
  private def pure(r: AnyRef): PureResult = r match
    case p: PureResult => p
    case f: FullResult => f.pure
    case _             => throw IllegalArgumentException(s"Unknown result type: ${r.getClass}")

  private def full(r: AnyRef): Option[FullResult] = r match
    case f: FullResult => Some(f)
    case _             => None

  private def rankColor(r: AnyRef): String =
    val p = pure(r)
    if p.hasFailure then Theme.TextDim
    else currentRanks.get((p.algoName, p.pattern, p.size)) match
      case Some(1)           => Theme.AccentSuccess
      case Some(2)           => Theme.AccentSecondary
      case Some(n) if n >= 7 => Theme.AccentDanger
      case _                 => Theme.TextNormal

  private def pctColor(r: AnyRef): String =
    val pct = controller.pctVsFastest(pure(r))
    if pct == "fastest" then Theme.AccentSuccess
    else if pct == "—"  then Theme.TextDim
    else
      val ratio = pct.dropRight(1).toDoubleOption.getOrElse(1.0)
      if ratio < 2.0 then Theme.AccentSecondary else Theme.AccentDanger

  // ── Columns ───────────────────────────────────────────────
  private def pureColumns: Seq[TableColumn[AnyRef, String]] = Seq(
    col("Algorithm",  110, r => pure(r).algoName,
      r => if pure(r).hasFailure then Theme.AccentDanger else Theme.TextBright),
    col("Pattern",    100, r => pure(r).pattern),
    col("Size",        50, r => pure(r).size.toString),
    col("Rank",        45, r => currentRanks.get((pure(r).algoName, pure(r).pattern, pure(r).size))
      .map(n => s"#$n").getOrElse("—"), rankColor),
    col("vs Fastest",  75, r => controller.pctVsFastest(pure(r)), pctColor),
    col("Mean ms",     75, r => f"${pure(r).meanNs / 1e6}%.3f",
      r => if pure(r).hasFailure then Theme.AccentDanger else Theme.TextBright),
    col("Median ms",   75, r => f"${pure(r).medianNs / 1e6}%.3f"),
    col("P99 ms",      70, r => f"${pure(r).p99Ns / 1e6}%.3f",
      r => if pure(r).p99Ns > pure(r).meanNs * 3 then Theme.AccentDanger else Theme.TextNormal),
    col("StdDev ms",   70, r => f"${pure(r).stdDevNs / 1e6}%.3f"),
    col("Throughput",  85, r => f"${pure(r).throughputElemsPerMs}%.0f el/ms"),
    col("Comparisons", 90, r => f"${pure(r).comparisons}%,d"),
    col("Swaps",       70, r => f"${pure(r).swaps}%,d"),
    col("Sorted?",     60,
      r => if pure(r).hasFailure then "FAIL" else if pure(r).isSorted then "✓" else "✗",
      r => if pure(r).hasFailure || !pure(r).isSorted then Theme.AccentDanger else Theme.AccentSuccess)
  )

  private def fullColumns: Seq[TableColumn[AnyRef, String]] = Seq(
    col("Heap MB",    70, r => full(r).map(f => f"${f.heapDeltaMb}%.2f").getOrElse("—")),
    col("Alloc MB/s", 80, r => full(r).map(f => f"${f.allocRateMbS}%.1f").getOrElse("—")),
    col("GC runs",    55, r => full(r).map(f => f.gcCollections.toString).getOrElse("—")),
    col("GC ms",      55, r => full(r).map(f => f.gcPauseMs.toString).getOrElse("—")),
    col("CPU %",      60, r => full(r).map(f => f"${f.cpuPercent}%.1f").getOrElse("—")),
    col("Stable?",    60,
      r => if pure(r).isStable then "✓" else "—",
      r => if pure(r).isStable then Theme.AccentSuccess else Theme.TextDim)
  )

  // ── Table node ────────────────────────────────────────────
  val tableView: TableView[AnyRef] = new TableView[AnyRef](results):
    style = s"-fx-background-color: ${Theme.BgBase};"
    columnResizePolicy = TableView.ConstrainedResizePolicy
    VBox.setVgrow(this, Priority.Always)

  tableView.delegate.getStylesheets.add(tableCss)
  applyColumns(BenchmarkMode.Pure)

  results.onChange { (_, _) =>
    Platform.runLater {
      currentRanks = controller.computeRanks()
      tableView.refresh()
    }
  }

  def setMode(mode: BenchmarkMode): Unit =
    if mode != currentMode then
      currentMode = mode
      applyColumns(mode)

  private def applyColumns(mode: BenchmarkMode): Unit =
    tableView.columns.clear()
    val cols = mode match
      case BenchmarkMode.Pure => pureColumns
      case BenchmarkMode.Full => pureColumns ++ fullColumns
    tableView.columns ++= cols.map(_.delegate)

  private def tableCss: String =
    "data:text/css," + java.net.URLEncoder.encode(
      s""".table-view { -fx-background-color: ${Theme.BgBase}; }
         |.table-view .column-header-background { -fx-background-color: ${Theme.BgDeep}; }
         |.table-view .column-header {
         |  -fx-background-color: ${Theme.BgDeep};
         |  -fx-border-color: ${Theme.BgBorder}; -fx-border-width: 0 1 1 0;
         |}
         |.table-view .column-header .label {
         |  -fx-text-fill: ${Theme.TextDim}; -fx-font-family: 'Consolas', monospace;
         |  -fx-font-size: 10px; -fx-font-weight: bold;
         |}
         |.table-row-cell {
         |  -fx-background-color: ${Theme.BgBase};
         |  -fx-border-color: transparent transparent ${Theme.BgBorder} transparent;
         |  -fx-table-cell-border-color: transparent;
         |}
         |.table-row-cell:odd      { -fx-background-color: ${Theme.BgRaised}; }
         |.table-row-cell:selected { -fx-background-color: ${Theme.BgHover}; }
         |.table-row-cell:hover    { -fx-background-color: ${Theme.BgHover}; }
         |.table-cell {
         |  -fx-text-fill: ${Theme.TextNormal}; -fx-font-family: 'Consolas', monospace;
         |  -fx-font-size: 11px; -fx-border-color: transparent;
         |}""".stripMargin,
      "UTF-8").replace("+", "%20")