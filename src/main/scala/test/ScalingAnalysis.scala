package test

import benchmark.{BenchmarkRunner, BenchmarkResult}
import model.{AlgorithmType, GeneratorType}
import java.io.{File, PrintWriter}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Multi-scale performance testing utility.
 * Tests QuickSort at 5 different array sizes to identify scaling bottlenecks.
 *
 * Usage: sbt "runMain test.ScalingAnalysis"
 * Output: scaling_analysis_[timestamp].csv
 */
object ScalingAnalysis:

  def main(args: Array[String]): Unit =
    val timestamp = LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val reportFile = s"scaling_analysis_$timestamp.csv"
    val pw = new PrintWriter(new File(reportFile), "UTF-8")

    try
      // CSV header
      pw.println("Array Size,Runs,Time Mean (ns),Time Median (ns),Time StdDev (ns),P90 (ns),P95 (ns),P99 (ns)," +
        "Throughput (elem/ms),GC Collections,GC Pause (ms),CPU %,Memory Δ (MB),Alloc Rate (MB/s)")

      val sizes = Seq(100, 500, 1000, 5000, 10000, 50000)

      sizes.foreach { size =>
        println(s"\n{'='*60}")
        println(s"Testing array size: $size elements")
        println(s"{'='*60}")

        val config = BenchmarkRunner.RunConfig(
          algo      = AlgorithmType.QuickSort,
          generator = GeneratorType.Random,
          size      = size,
          warmup    = true
        )

        val results = BenchmarkRunner.run(config, message => println(s"  > $message"))

        // Aggregate result should be last (or only if warmup=true)
        val aggResult = results.lastOption.getOrElse(throw new Exception("No results"))

        val throughputElemMs = if aggResult.timeNs > 0 then
          size.toDouble / (aggResult.timeNs.toDouble / 1_000_000_000.0) / 1_000_000.0
        else
          0.0

        // Print to console
        println()
        println(f"  Array Size:      $size%,d elements")
        println(f"  Mean Time:       ${aggResult.timeMeanNs}%,d ns")
        println(f"  Median Time:     ${aggResult.timeMedianNs}%,d ns")
        println(f"  StdDev:          ${aggResult.timeStdDevNs}%.1f ns")
        println(f"  P90:             ${aggResult.timeP90Ns}%,d ns")
        println(f"  P95:             ${aggResult.timeP95Ns}%,d ns")
        println(f"  Throughput:      $throughputElemMs%.1f elem/ms")
        println(f"  GC Collections:  ${aggResult.gcCollections.toInt} (avg per run)")
        println(f"  GC Pause:        ${aggResult.gcPauseMs}%.2f ms")
        println(f"  CPU Usage:       ${aggResult.cpuPercent}%.1f%%")
        println(f"  Memory Δ:        ${aggResult.maxHeapMb}%.1f MB")

        // Write to CSV
        pw.println(f"$size,${results.length}," +
          f"${aggResult.timeMeanNs}," +
          f"${aggResult.timeMedianNs}," +
          f"${aggResult.timeStdDevNs}%.1f," +
          f"${aggResult.timeP90Ns}," +
          f"${aggResult.timeP95Ns}," +
          f"${aggResult.timeP99Ns}," +
          f"$throughputElemMs%.1f," +
          f"${aggResult.gcCollections.toInt}," +
          f"${aggResult.gcPauseMs}%.2f," +
          f"${aggResult.cpuPercent}%.1f," +
          f"${aggResult.maxHeapMb}%.1f," +
          f"${aggResult.allocRateMbS}%.1f")
        pw.flush()

        Thread.sleep(500)  // Brief pause between sizes
      }

      println("\n" + "="*60)
      println(s"Report saved to: $reportFile")
      println("="*60)

      // Print analysis
      printAnalysis(reportFile)

    finally
      pw.close()

  private def printAnalysis(filePath: String): Unit =
    println("\nScaling Analysis:")
    println("-" * 60)

    val lines = scala.io.Source.fromFile(filePath).getLines().drop(1).toList

    if lines.length >= 2 then
      val first = lines.head.split(",")
      val last = lines.last.split(",")

      val size1 = first(0).toInt
      val throughput1 = first(8).toDouble

      val sizeN = last(0).toInt
      val throughputN = last(8).toDouble

      val sizeRatio = sizeN.toDouble / size1
      val throughputRatio = throughputN / throughput1

      // For O(n log n), throughput should decrease logarithmically
      // throughput_ratio ≈ size_ratio * log(size1) / log(sizeN)
      val expectedRatio = (Math.log(size1) / Math.log(sizeN))
      val actualRatio = throughputRatio
      val deviation = Math.abs(actualRatio - expectedRatio) / expectedRatio * 100

      println(f"\nSize scaling: $size1 → $sizeN (${sizeRatio.toInt}x increase)")
      println(f"Throughput change: $throughput1%.1f → $throughputN%.1f elem/ms (${throughputRatio}x)")
      println()
      println("Expected O(n log n) behavior:")
      println(f"  Throughput ratio should decrease ~${expectedRatio.toString.take(5)}")
      println(f"  Actual ratio: ${actualRatio.toString.take(5)}")
      println(f"  Deviation: ${deviation.toString.take(5)}")

      if deviation < 20 then
        println("\n✓ SCALING IS NORMAL - Algorithm appears O(n log n)")
      else if deviation < 50 then
        println("\n⚠ SCALING DEGRADATION - May indicate memory or GC issues")
      else
        println("\n✗ SEVERE DEGRADATION - Check for O(n²) or cache thrashing")

