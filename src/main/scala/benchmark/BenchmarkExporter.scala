package benchmark

import java.io.{File, PrintWriter}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import monitoring.{HardwareInfo as MonitoringHardwareInfo, ExcelReporter}

object BenchmarkExporter:

  private def timestamp: String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

  def exportExcel(results: Seq[BenchmarkResult], dir: String = "."): String =
    // Get hardware profile snapshot (one-time, after benchmark completion)
    val hardwareProfile = monitoring.HardwareInfo.collectProfileSafely()

    // Create Excel file with 2 sheets: Benchmark and Hardware
    ExcelReporter.exportToExcel(results, hardwareProfile, s"$dir/benchmark_$timestamp.xlsx").get

  def exportJson(results: Seq[BenchmarkResult], dir: String = "."): String =
    val file = new File(s"$dir/benchmark_$timestamp.json")
    val pw   = new PrintWriter(file)
    try
      val hw = HardwareInfo.snapshot()
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
      results.zipWithIndex.foreach { (r, idx) =>
        val comma = if idx < results.size - 1 then "," else ""
        pw.println(s"""  {
                      |    "algoName": "${r.algoName}",
                      |    "variant": "${r.variant}",
                      |    "pattern": "${r.pattern}",
                      |    "size": ${r.size},
                      |    "isWarm": ${r.isWarm},
                      |    "time": {
                      |      "ns": ${r.timeNs},
                      |      "meanNs": ${r.timeMeanNs},
                      |      "medianNs": ${r.timeMedianNs},
                      |      "p90Ns": ${r.timeP90Ns},
                      |      "p95Ns": ${r.timeP95Ns},
                      |      "p99Ns": ${r.timeP99Ns},
                      |      "minNs": ${r.timeMinNs},
                      |      "maxNs": ${r.timeMaxNs},
                      |      "stdDevNs": ${f"${r.timeStdDevNs}%.2f"}
                      |    },
                      |    "throughput": ${f"${r.throughput}%.2f"},
                      |    "opsPerSec": ${f"${r.opsPerSec}%.2f"},
                      |    "algorithmic": {
                      |      "comparisons": ${r.comparisons},
                      |      "swaps": ${r.swaps},
                      |      "writes": ${r.writes}
                      |    },
                      |    "memory": {
                      |      "heapDeltaMb": ${f"${r.maxHeapMb}%.4f"},
                      |      "allocRateMbS": ${f"${r.allocRateMbS}%.4f"},
                      |      "gcCollections": ${r.gcCollections},
                      |      "gcPauseMs": ${r.gcPauseMs}
                      |    },
                      |    "cpu": {
                      |      "cpuTimeNs": ${r.cpuTimeNs},
                      |      "userTimeNs": ${r.userTimeNs},
                      |      "cpuPercent": ${f"${r.cpuPercent}%.2f"}
                      |    },
                      |    "correctness": {
                      |      "isSorted": ${r.isSorted},
                      |      "isStable": ${r.isStable},
                      |      "failureMsg": "${r.failureMsg.replace("\"", "\\\"")}"
                      |    }
                      |  }$comma""".stripMargin)
      }
      pw.println("  ]")
      pw.println("}")
      file.getAbsolutePath
    finally
      pw.close()
