package benchmark

import java.lang.management.ManagementFactory
import scala.util.Try

object HardwareInfo:

  private val osBean = ManagementFactory.getOperatingSystemMXBean

  private val sunOsBeanOpt: Option[com.sun.management.OperatingSystemMXBean] =
    osBean match
      case b: com.sun.management.OperatingSystemMXBean => Some(b)
      case _                                            => None

  def osName: String    = System.getProperty("os.name", "Unknown")
  def osVersion: String = System.getProperty("os.version", "Unknown")
  def osArch: String    = System.getProperty("os.arch", "Unknown")

  def cpuLogicalCores: Int = Runtime.getRuntime.availableProcessors()

  def cpuName: String =
    val os = osName.toLowerCase
    if os.contains("win") then
      Option(System.getenv("PROCESSOR_IDENTIFIER")).getOrElse("Unknown")
    else if os.contains("linux") then
      Try {
        val lines = scala.io.Source.fromFile("/proc/cpuinfo").getLines()
        lines
          .find(_.startsWith("model name"))
          .map(_.split(":").last.trim)
          .getOrElse("Unknown")
      }.getOrElse("Unknown")
    else if os.contains("mac") then
      Try {
        val proc = Runtime.getRuntime.exec(Array("sysctl", "-n", "machdep.cpu.brand_string"))
        val out  = scala.io.Source.fromInputStream(proc.getInputStream).mkString.trim
        if out.nonEmpty then out else "Unknown"
      }.getOrElse("Unknown")
    else "Unknown"

  def totalRamMb: Long =
    sunOsBeanOpt
      .map(b => b.getTotalMemorySize / (1024L * 1024L))
      .getOrElse(-1L)

  def jvmName: String    = System.getProperty("java.vm.name", "Unknown")
  def jvmVersion: String = System.getProperty("java.version", "Unknown")
  def jvmHeapMaxMb: Long = Runtime.getRuntime.maxMemory() / (1024L * 1024L)

  case class Snapshot(
    osName:          String,
    osVersion:       String,
    osArch:          String,
    cpuName:         String,
    cpuLogicalCores: Int,
    totalRamMb:      Long,
    jvmName:         String,
    jvmVersion:      String,
    jvmHeapMaxMb:    Long
  )

  def snapshot(): Snapshot = Snapshot(
    osName          = osName,
    osVersion       = osVersion,
    osArch          = osArch,
    cpuName         = cpuName,
    cpuLogicalCores = cpuLogicalCores,
    totalRamMb      = totalRamMb,
    jvmName         = jvmName,
    jvmVersion      = jvmVersion,
    jvmHeapMaxMb    = jvmHeapMaxMb
  )
