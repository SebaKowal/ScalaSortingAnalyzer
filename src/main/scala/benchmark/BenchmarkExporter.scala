package benchmark

import java.io.{File, PrintWriter}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import monitoring.{ExcelReporter, MonitoringHardwareInfo}
import benchmark.pure.PureResult
import benchmark.full.FullResult

object BenchmarkExporter:

  private def timestamp: String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

  // ── EXCEL ────────────────────────────────────────────────────────────────

  /** Eksport dla trybu Pure - tylko czas i operacje */
  def exportPureExcel(results: Seq[PureResult], dir: String = "."): String =
    val hardwareProfile = monitoring.HardwareInfo.collectProfileSafely()
    // Tutaj ExcelReporter dostaje tylko to, co ma PureResult
    ExcelReporter.exportPureToExcel(results, hardwareProfile, s"$dir/benchmark_pure_$timestamp.xlsx").get

  /** Eksport dla trybu Full - pełne metryki systemowe */
  def exportFullExcel(results: Seq[FullResult], dir: String = "."): String =
    val hardwareProfile = monitoring.HardwareInfo.collectProfileSafely()
    // Tutaj ExcelReporter dostaje pełny FullResult (z metrykami CPU/RAM)
    ExcelReporter.exportFullToExcel(results, hardwareProfile, s"$dir/benchmark_full_$timestamp.xlsx").get

  // ── JSON ─────────────────────────────────────────────────────────────────

  def exportJson(results: Seq[AnyRef], dir: String = "."): String =
    val file = new File(s"$dir/benchmark_$timestamp.json")
    val pw   = new PrintWriter(file)
    try
      val hw = MonitoringHardwareInfo.snapshot()
      pw.println("{")
      pw.println(s"""  "hardware": {""")
      pw.println(s"""    "os": "${hw.osName} ${hw.osVersion} (${hw.osArch})",""")
      pw.println(s"""    "cpu": "${hw.cpuName.replace("\"", "\\\"")}",""")
      pw.println(s"""    "cpuLogicalCores": ${hw.cpuLogicalCores},""")
      pw.println(s"""    "ramTotalMb": ${hw.totalRamMb},""")
      pw.println(s"""    "jvm": "${hw.jvmName} ${hw.jvmVersion}",""")
      pw.println(s"""    "jvmHeapMaxMb": ${hw.jvmHeapMaxMb}""")
      pw.println(s"""  },""")
      pw.println(s"""  "results": [""")

      results.zipWithIndex.foreach { (res, idx) =>
        val comma = if idx < results.size - 1 then "," else ""

        // Pattern matching decyduje co zapisać w JSONie
        val jsonChunk = res match
          case r: FullResult => formatFullJson(r)
          case r: PureResult => formatPureJson(r)
          case _             => "{}"

        pw.println(jsonChunk + comma)
      }

      pw.println("  ]")
      pw.println("}")
      file.getAbsolutePath
    finally
      pw.close()

  // ── HELPERS ──────────────────────────────────────────────────────────────

  private def formatPureJson(r: PureResult): String =
    s"""  {
       |    "algoName": "${r.algoName}",
       |    "variant": "Pure",
       |    "pattern": "${r.pattern}",
       |    "size": ${r.size},
       |    "time": { "meanNs": ${r.meanNs}, "medianNs": ${r.medianNs} },
       |    "algorithmic": { "comparisons": ${r.comparisons}, "swaps": ${r.swaps} },
       |    "correctness": { "isSorted": ${r.isSorted} }
       |  }""".stripMargin

  private def formatFullJson(r: FullResult): String =
    s"""  {
       |    "algoName": "${r.algoName}",
       |    "variant": "Full",
       |    "pattern": "${r.pattern}",
       |    "size": ${r.size},
       |    "time": { "meanNs": ${r.meanNs}, "medianNs": ${r.medianNs} },
       |    "memory": { "heapDeltaMb": ${f"${r.heapDeltaMb}%.4f"}, "gcCollections": ${r.gcCollections} },
       |    "cpu": { "cpuPercent": ${f"${r.cpuPercent}%.2f"} },
       |    "correctness": { "isSorted": ${r.isSorted} }
       |  }""".stripMargin