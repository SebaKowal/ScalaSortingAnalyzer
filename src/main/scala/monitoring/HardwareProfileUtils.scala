package monitoring

import scala.util.{Try, Success, Failure}

/**
 * Integration utility for incorporating hardware profiling into benchmark workflows.
 * Provides convenient methods for capturing and reporting baseline configurations.
 */
object HardwareProfileUtils:

  /**
   * Quick capture: collect hardware profile and export to Excel immediately.
   *
   * @param outputDir Directory where Excel file will be saved
   * @return Try containing the file path
   */
  def captureAndExport(outputDir: String = "."): Try[String] =
    Try(ExcelReporter.exportToExcel(HardwareInfo.collectProfileSafely(),
      s"$outputDir/hardware_profile.xlsx").get)

  /**
   * Print hardware profile to console (useful for documentation).
   *
   * @return The captured profile
   */
  def captureAndPrint(): HardwareProfile =
    val profile = HardwareInfo.collectProfileSafely()
    printProfile(profile)
    profile

  /**
   * Pretty-print hardware profile.
   */
  def printProfile(profile: HardwareProfile): Unit =
    println("\n" + "=" * 80)
    println("HARDWARE PROFILE - BASELINE SNAPSHOT FOR SORTING ALGORITHM BENCHMARKS")
    println("=" * 80)

    println("\n[CPU]")
    println(f"  Model:           ${profile.cpuModel}")
    println(f"  Vendor:          ${profile.cpuVendor}")
    println(f"  Physical Cores:  ${profile.physicalCores}")
    println(f"  Logical Cores:   ${profile.logicalCores} (${if profile.smtEnabled.contains(true) then "SMT/HT Enabled" else "SMT/HT Disabled"})")
    println(f"  Base Frequency:  ${profile.currentFrequencyMhz}%.2f MHz")
    println(f"  Max Frequency:   ${profile.maxFrequencyMhz}%.2f MHz")
    println(f"  Cache L1:        ${profile.l1CacheKb.getOrElse("Unknown")} KB")
    println(f"  Cache L2:        ${profile.l2CacheKb.getOrElse("Unknown")} KB")
    println(f"  Cache L3:        ${profile.l3CacheKb.getOrElse("Unknown")} KB")

    println("\n[MEMORY]")
    println(f"  Total RAM:       ${profile.totalRamGb}%.2f GB")
    println(f"  Available:       ${profile.availableRamGb}%.2f GB")
    println(f"  Type:            ${profile.ramType}")
    println(f"  Speed:           ${profile.ramSpeedMts.getOrElse("Unknown")} MT/s")

    println("\n[OPERATING SYSTEM]")
    println(f"  Name:            ${profile.osName}")
    println(f"  Version:         ${profile.osVersion}")
    println(f"  Architecture:    ${profile.osArch}")

    println("\n[JAVA VIRTUAL MACHINE]")
    println(f"  Vendor:          ${profile.jvmVendor}")
    println(f"  Version:         ${profile.jvmVersion}")
    println(f"  Runtime:         ${profile.jvmRuntime}")
    println(f"  Xmx (Max Heap):  ${profile.jvmXmxMb} MB")
    println(f"  Xms (Init Heap): ${profile.jvmXmsMb} MB")
    println(f"  GC Type:         ${profile.gcType}")

    println("\n[MOTHERBOARD / BIOS]")
    println(f"  Manufacturer:    ${profile.biosManufacturer.getOrElse("Unknown")}")
    println(f"  Model:           ${profile.biosModel.getOrElse("Unknown")}")
    println(f"  BIOS Version:    ${profile.biosVersion.getOrElse("Unknown")}")

    println("\n[STORAGE DEVICES]")
    if profile.storageDevices.isEmpty then
      println("  No devices found")
    else
      profile.storageDevices.zipWithIndex.foreach { case (device, idx) =>
        println(f"  Device ${idx + 1}:")
        println(f"    Model:     ${device.model}")
        println(f"    Interface: ${device.interface}")
        println(f"    Capacity:  ${device.capacityGb} GB")
      }

    println(f"\n[CAPTURED AT]")
    println(f"  ${profile.capturedAt}")
    println("\n" + "=" * 80)

