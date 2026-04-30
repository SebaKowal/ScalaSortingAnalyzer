package benchmark

import java.io.{File, PrintWriter}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import monitoring.{HardwareInfo as MonitoringHardwareInfo, ExcelReporter}
import scala.util.Try

object BenchmarkExporter:

  private def timestamp: String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

  private def writeHardwareMetadata(pw: java.io.PrintWriter, hw: HardwareInfo.Snapshot, prefix: String): Unit =
    pw.println(s"${prefix}os: ${hw.osName} ${hw.osVersion} (${hw.osArch})")
    pw.println(s"${prefix}cpu: ${hw.cpuName} (${hw.cpuLogicalCores} logical cores)")
    pw.println(s"${prefix}ram_total_mb: ${hw.totalRamMb}")
    pw.println(s"${prefix}jvm: ${hw.jvmName} ${hw.jvmVersion} (heap max ${hw.jvmHeapMaxMb} MB)")

  def exportCsv(results: Seq[BenchmarkResult], dir: String = "."): String =
    val file = new File(s"$dir/benchmark_$timestamp.csv")
    val pw   = new PrintWriter(file, "UTF-8")
    try
      // Write UTF-8 BOM for Excel compatibility
      pw.write('\ufeff')

      // Get comprehensive hardware profile
      val hardwareProfile = MonitoringHardwareInfo.collectProfileSafely()
      val hardwareRows = ExcelReporter.prepareDataRows(hardwareProfile)

      // Write hardware profile section
      pw.println("Parametr;Wartość;Jednostka")
      hardwareRows.foreach { row =>
        val formattedRow = row.map {
          case s: String => s""""${s.replace("\"", "\"\"")}""""
          case n: Number => n.toString
          case b: Boolean => if b then "Tak" else "Nie"
          case other => other.toString
        }
        pw.println(formattedRow.mkString(";"))
      }

      // Separator between hardware and benchmark data
      pw.println("\"\";\"\";\"\"")
      pw.println("\"=== WYNIKI BENCHMARKÓW ===\";\"\";\"\"")
      pw.println("\"\";\"\";\"\"")

      // Write benchmark results header
      pw.println(
        "algoName,variant,pattern,size,isWarm,timeNs,timeMeanNs,timeMedianNs," +
          "timeP90Ns,timeP95Ns,timeP99Ns,timeMinNs,timeMaxNs,timeStdDevNs," +
          "throughput,opsPerSec,comparisons,swaps,writes," +
          "heapDeltaMb,allocRateMbS,gcCollections,gcPauseMs," +
          "cpuTimeNs,userTimeNs,cpuPercent,isSorted,isStable,failureMsg"
      )

      // Write benchmark results
      results.foreach { r =>
        pw.println(Seq(
          r.algoName, r.variant, r.pattern, r.size, r.isWarm,
          r.timeNs, r.timeMeanNs, r.timeMedianNs,
          r.timeP90Ns, r.timeP95Ns, r.timeP99Ns, r.timeMinNs, r.timeMaxNs,
          f"${r.timeStdDevNs}%.2f",
          f"${r.throughput}%.2f", f"${r.opsPerSec}%.2f",
          r.comparisons, r.swaps, r.writes,
          f"${r.maxHeapMb}%.4f", f"${r.allocRateMbS}%.4f",
          r.gcCollections, r.gcPauseMs,
          r.cpuTimeNs, r.userTimeNs, f"${r.cpuPercent}%.2f",
          r.isSorted, r.isStable,
          s""""${r.failureMsg.replace("\"", "\"\"")}""""
        ).mkString(","))
      }
      file.getAbsolutePath
    finally
      pw.close()

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

  /**
   * Export comprehensive hardware profile to Excel file.
   * Used for baseline documentation before benchmarking.
   */
  def exportHardwareProfileToExcel(dir: String = "."): Try[String] =
    Try {
      val profile = MonitoringHardwareInfo.collectProfileSafely()
      val fileName = s"$dir/hardware_profile_$timestamp.csv"
      ExcelReporter.exportToExcel(profile, fileName).get
    }
