package monitoring

import scala.util.{Try, Failure, Success}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Command-line tool for generating hardware baseline profiles.
 * Usage: scala monitoring.HardwareProfiler [output-filename.xlsx]
 */
object HardwareProfiler:

  def main(args: Array[String]): Unit =
    val timestamp = LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
    val outputFile = if args.nonEmpty then
      args(0)
    else
      s"./hardware_profile_${timestamp}.csv"

    println("=" * 70)
    println("Hardware Profile Collector for Sorting Algorithm Benchmarking")
    println("=" * 70)
    println()

    try {
      println("Collecting hardware information...")
      println("-" * 70)

      val profile = HardwareInfo.collectProfileSafely()

      println(s"✓ CPU: ${profile.cpuModel}")
      println(s"✓ Cores: ${profile.physicalCores}P/${profile.logicalCores}L")
      println(s"✓ RAM: ${profile.totalRamGb} GB")
      println(s"✓ JVM: ${profile.jvmVersion}")
      println(s"✓ GC: ${profile.gcType}")
      println(s"✓ Disks: ${profile.storageDevices.length} device(s)")
      println()

      println("Generating Excel report...")
      println("-" * 70)

      ExcelReporter.exportToExcel(profile, outputFile) match {
        case Success(filePath) =>
          println(s"✓ Report saved to: $filePath")
          println()
          println("Profile Details:")
          println("=" * 70)
          printProfileSummary(profile)
          println("=" * 70)
          println("Report generation completed successfully!")

        case Failure(exception) =>
          println(s"✗ Failed to generate report: ${exception.getMessage}")
          exception.printStackTrace()
      }
    } catch {
      case e: Exception =>
        println(s"✗ Error: ${e.getMessage}")
        e.printStackTrace()
    }

  private def printProfileSummary(profile: HardwareProfile): Unit =
    println()
    println("PROCESSOR:")
    println(s"  Model: ${profile.cpuModel}")
    println(s"  Vendor: ${profile.cpuVendor}")
    println(s"  Physical Cores: ${profile.physicalCores}")
    println(s"  Logical Cores: ${profile.logicalCores}")
    println(s"  Max Frequency: ${profile.maxFrequencyMhz} MHz")
    println(s"  SMT/HT Enabled: ${profile.smtEnabled.map(b => if b then "Yes" else "No").getOrElse("Unknown")}")

    println()
    println("MEMORY:")
    println(f"  Total RAM: ${profile.totalRamGb}%.2f GB")
    println(f"  Available: ${profile.availableRamGb}%.2f GB")
    println(s"  Type: ${profile.ramType}")
    println(s"  Speed: ${profile.ramSpeedMts.getOrElse("Unknown")} MT/s")

    println()
    println("OPERATING SYSTEM:")
    println(s"  Name: ${profile.osName}")
    println(s"  Version: ${profile.osVersion}")
    println(s"  Architecture: ${profile.osArch}")

    println()
    println("JAVA VIRTUAL MACHINE:")
    println(s"  Vendor: ${profile.jvmVendor}")
    println(s"  Version: ${profile.jvmVersion}")
    println(s"  Runtime: ${profile.jvmRuntime}")
    println(f"  Max Memory (Xmx): ${profile.jvmXmxMb} MB")
    println(f"  Initial Memory (Xms): ${profile.jvmXmsMb} MB")
    println(s"  Garbage Collector: ${profile.gcType}")

    println()
    println("MOTHERBOARD & BIOS:")
    println(s"  Manufacturer: ${profile.biosManufacturer.getOrElse("Unknown")}")
    println(s"  Model: ${profile.biosModel.getOrElse("Unknown")}")
    println(s"  BIOS Version: ${profile.biosVersion.getOrElse("Unknown")}")

    println()
    println("STORAGE DEVICES:")
    if profile.storageDevices.isEmpty then
      println("  No devices found")
    else
      profile.storageDevices.zipWithIndex.foreach { (device, idx) =>
        println(s"  Device ${idx + 1}:")
        println(s"    Model: ${device.model}")
        println(s"    Interface: ${device.interface}")
        println(s"    Capacity: ${device.capacityGb} GB")
      }

    println()
    println(s"Captured at: ${profile.capturedAt}")
