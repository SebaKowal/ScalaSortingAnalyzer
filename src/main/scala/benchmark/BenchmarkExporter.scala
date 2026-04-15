package benchmark

import java.io.{File, PrintWriter}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object BenchmarkExporter:

  private def timestamp: String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))

  def exportCsv(results: Seq[BenchmarkResult], dir: String = "."): String =
    val file = new File(s"$dir/benchmark_$timestamp.csv")
    val pw   = new PrintWriter(file)
    try
      // Header
      pw.println(
        "algoName,variant,pattern,size,isWarm,timeNs,timeMeanNs,timeMedianNs," +
          "timeP90Ns,timeP95Ns,timeP99Ns,timeMinNs,timeMaxNs,timeStdDevNs," +
          "throughput,opsPerSec,comparisons,swaps,writes," +
          "heapDeltaMb,allocRateMbS,gcCollections,gcPauseMs," +
          "cpuTimeNs,userTimeNs,cpuPercent,isSorted,isStable,failureMsg"
      )
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
      pw.println("[")
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
      pw.println("]")
      file.getAbsolutePath
    finally
      pw.close()