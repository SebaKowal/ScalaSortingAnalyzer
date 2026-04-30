package monitoring

import scala.util.{Try, Using}
import scala.jdk.CollectionConverters.*
import java.lang.management.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.sun.management.OperatingSystemMXBean
import scala.sys.process.*

/**
 * Collects comprehensive hardware and system information from JVM and OS.
 * Provides detailed metrics for baseline documentation before benchmarking.
 */
object HardwareInfo:

  /**
   * Collect all available hardware information.
   */
  def collectProfile(): HardwareInfo =
    HardwareInfo()

  /**
   * Collect only available hardware information without throwing exceptions.
   */
  def collectProfileSafely(): HardwareProfile =
    val now = LocalDateTime.now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

    HardwareProfile(
      // CPU
      cpuModel = Try(getCpuModel).getOrElse("Unknown"),
      cpuVendor = Try(getCpuVendor).getOrElse("Unknown"),
      physicalCores = getPhysicalCores,
      logicalCores = getLogicalCores,
      currentFrequencyMhz = getCurrentFrequency,
      maxFrequencyMhz = getMaxFrequency,
      processorId = Try(getProcessorId).getOrElse("Unknown"),
      l1CacheKb = Try(getL1Cache).toOption.flatMap(v => if v > 0 then Some(v.toString) else None),
      l2CacheKb = Try(getL2Cache).toOption.flatMap(v => if v > 0 then Some(v.toString) else None),
      l3CacheKb = Try(getL3Cache).toOption.flatMap(v => if v > 0 then Some(v.toString) else None),
      smtEnabled = Try(isSmtEnabled).toOption,

      // RAM
      totalRamGb = getTotalRam,
      availableRamGb = getAvailableRam,
      ramType = Try(getRamType).getOrElse("Unknown"),
      ramSpeedMts = Try(getRamSpeed).toOption.flatMap(v => if v > 0 then Some(v.toString) else None),

      // OS
      osName = System.getProperty("os.name"),
      osVersion = System.getProperty("os.version"),
      osArch = System.getProperty("os.arch"),

      // JVM
      jvmVendor = System.getProperty("java.vendor"),
      jvmVersion = System.getProperty("java.version"),
      jvmRuntime = s"${System.getProperty("java.vm.name")} ${System.getProperty("java.vm.version")}",
      jvmXmxMb = getJvmMaxMemory,
      jvmXmsMb = getJvmInitialMemory,
      gcType = getGarbageCollectorInfo,

      // BIOS/Motherboard
      biosManufacturer = Try(getBiosManufacturer).toOption,
      biosModel = Try(getBiosModel).toOption,
      biosVersion = Try(getBiosVersion).toOption,

      // Storage
      storageDevices = Try(getStorageDevices).getOrElse(List.empty),

      capturedAt = now
    )

  // ==================== CPU Information ====================

  private def getCpuModel: String =
    val cmd = if System.getProperty("os.name").toLowerCase.contains("win") then
      """powershell -NoProfile -Command "Get-WmiObject Win32_Processor | Select-Object -ExpandProperty Name""""
    else
      "grep -m1 'model name' /proc/cpuinfo | cut -d: -f2"

    Try(cmd.!!.trim).getOrElse("Unknown")

  private def getCpuVendor: String =
    val cmd = if System.getProperty("os.name").toLowerCase.contains("win") then
      """powershell -NoProfile -Command "Get-WmiObject Win32_Processor | Select-Object -ExpandProperty Manufacturer""""
    else
      "grep -m1 'vendor_id' /proc/cpuinfo | cut -d: -f2"

    Try(cmd.!!.trim).getOrElse("Unknown")

  private def getPhysicalCores: Int =
    Runtime.getRuntime.availableProcessors / 2

  private def getLogicalCores: Int =
    Runtime.getRuntime.availableProcessors

  private def getCurrentFrequency: Double =
    getMaxFrequency * 0.8 // Estimate current frequency as 80% of maximum

  private def getMaxFrequency: Double =
    val cmd = if System.getProperty("os.name").toLowerCase.contains("win") then
      """powershell -NoProfile -Command "Get-WmiObject Win32_Processor | Select-Object -ExpandProperty MaxClockSpeed""""
    else
      "grep -m1 'cpu MHz' /proc/cpuinfo | awk '{print $4}'"

    Try(cmd.!!.trim.toDouble).getOrElse(0.0)

  private def getProcessorId: String =
    val cmd = if System.getProperty("os.name").toLowerCase.contains("win") then
      """powershell -NoProfile -Command "Get-WmiObject Win32_Processor | Select-Object -ExpandProperty ProcessorId""""
    else
      "grep -m1 'cpuid level' /proc/cpuinfo"

    Try(cmd.!!.trim).getOrElse("Unknown")

  private def getL1Cache: Long =
    val cmd = if System.getProperty("os.name").toLowerCase.contains("win") then
      """powershell -NoProfile -Command "Get-WmiObject Win32_Processor | Select-Object -ExpandProperty L2CacheSize""""
    else
      "grep -m1 'cache' /proc/cpuinfo"

    Try(cmd.!!.trim.toLong).getOrElse(0L)

  private def getL2Cache: Long =
    val cmd = if System.getProperty("os.name").toLowerCase.contains("win") then
      """powershell -NoProfile -Command "Get-WmiObject Win32_Processor | Select-Object -ExpandProperty L2CacheSize""""
    else
      "grep -m1 'cache size' /proc/cpuinfo | awk '{print $4}'"

    Try(cmd.!!.trim.toLong).getOrElse(0L)

  private def getL3Cache: Long =
    getL2Cache * 3 // Rough estimation

  private def isSmtEnabled: Boolean =
    getLogicalCores > getPhysicalCores

  // ==================== RAM Information ====================

  private def getTotalRam: Double =
    val osBean = ManagementFactory.getOperatingSystemMXBean.asInstanceOf[OperatingSystemMXBean]
    osBean.getTotalPhysicalMemorySize.toDouble / (1024 * 1024 * 1024)

  private def getAvailableRam: Double =
    val osBean = ManagementFactory.getOperatingSystemMXBean.asInstanceOf[OperatingSystemMXBean]
    osBean.getFreePhysicalMemorySize.toDouble / (1024 * 1024 * 1024)

  private def getRamType: String =
    val cmd = if System.getProperty("os.name").toLowerCase.contains("win") then
      """powershell -NoProfile -Command "Get-WmiObject Win32_PhysicalMemory | Select-Object -First 1 -ExpandProperty MemoryType""""
    else
      "grep -m1 'DDR' /proc/meminfo"

    Try(cmd.!!.trim).getOrElse("Unknown")

  private def getRamSpeed: Long =
    val cmd = if System.getProperty("os.name").toLowerCase.contains("win") then
      """powershell -NoProfile -Command "Get-WmiObject Win32_PhysicalMemory | Select-Object -First 1 -ExpandProperty Speed""""
    else
      "grep -m1 'Speed' /proc/meminfo"

    Try(cmd.!!.trim.toLong).getOrElse(0L)

  // ==================== JVM Information ====================

  private def getJvmMaxMemory: Long =
    Runtime.getRuntime.maxMemory / (1024 * 1024)

  private def getJvmInitialMemory: Long =
    Runtime.getRuntime.totalMemory / (1024 * 1024)

  private def getGarbageCollectorInfo: String =
    val gcs = ManagementFactory.getGarbageCollectorMXBeans.asScala
    gcs.map(_.getName).mkString(", ")

  // ==================== BIOS/Motherboard Information ====================

  private def getBiosManufacturer: String =
    val cmd = """powershell -NoProfile -Command "Get-WmiObject Win32_BIOS | Select-Object -ExpandProperty Manufacturer""""
    cmd.!!.trim

  private def getBiosModel: String =
    val cmd = """powershell -NoProfile -Command "Get-WmiObject Win32_BaseBoard | Select-Object -ExpandProperty Model""""
    cmd.!!.trim

  private def getBiosVersion: String =
    val cmd = """powershell -NoProfile -Command "Get-WmiObject Win32_BIOS | Select-Object -ExpandProperty Version""""
    cmd.!!.trim

  // ==================== Storage Information ====================

  private def getStorageDevices: List[StorageDevice] =
    val cmd = if System.getProperty("os.name").toLowerCase.contains("win") then
      """powershell -NoProfile -Command "Get-WmiObject Win32_DiskDrive | ForEach-Object { Write-Output \\\"$($_.Model)|$($_.InterfaceType)|$([math]::Round($_.Size/1GB))\\\" }""""
    else
      "lsblk -d -o NAME,MODEL,SIZE,ROTA"

    Try {
      cmd.!!.split("\n").filter(_.nonEmpty).map { line =>
        val parts = line.split("\\|")
        if parts.length >= 3 then
          StorageDevice(
            model = parts(0).trim,
            interface = parts(1).trim,
            capacityGb = Try(parts(2).trim.toLong).getOrElse(0L)
          )
        else
          StorageDevice("Unknown", "Unknown", 0)
      }.toList
    }.getOrElse(List.empty)

case class HardwareInfo():
  lazy val profile: HardwareProfile = HardwareInfo.collectProfileSafely()






