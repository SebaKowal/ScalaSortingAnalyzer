package benchmark

import java.lang.management.ManagementFactory
import scala.jdk.CollectionConverters.*

object SystemMonitor:

  private val memBean = ManagementFactory.getMemoryMXBean
  private val gcBeans = ManagementFactory.getGarbageCollectorMXBeans.asScala.toList

  private val osBeanOpt: Option[com.sun.management.OperatingSystemMXBean] =
    ManagementFactory.getOperatingSystemMXBean match
      case b: com.sun.management.OperatingSystemMXBean => Some(b)
      case _                                            => None

  def heapUsedMb: Double =
    memBean.getHeapMemoryUsage.getUsed.toDouble / (1024 * 1024)

  def heapMaxMb: Double =
    memBean.getHeapMemoryUsage.getMax.toDouble / (1024 * 1024)

  def heapCommittedMb: Double =
    memBean.getHeapMemoryUsage.getCommitted.toDouble / (1024 * 1024)

  def nonHeapUsedMb: Double =
    memBean.getNonHeapMemoryUsage.getUsed.toDouble / (1024 * 1024)

  def cpuLoadPercent: Double =
    osBeanOpt.map(b => (b.getProcessCpuLoad * 100).max(0)).getOrElse(0.0)

  def systemCpuLoadPercent: Double =
    osBeanOpt.map(b => (b.getCpuLoad * 100).max(0)).getOrElse(0.0)

  def totalGcCollections: Long =
    gcBeans.map(_.getCollectionCount).filter(_ >= 0).sum

  def totalGcTimeMs: Long =
    gcBeans.map(_.getCollectionTime).filter(_ >= 0).sum

  def gcDetails: List[(String, Long, Long)] =
    gcBeans.map(b => (b.getName, b.getCollectionCount.max(0), b.getCollectionTime.max(0)))

  def availableProcessors: Int =
    Runtime.getRuntime.availableProcessors()

  def jvmUptime: Long =
    ManagementFactory.getRuntimeMXBean.getUptime

  case class Snapshot(gcCollections: Long, gcTimeMs: Long, heapMb: Double)

  def snapshot(): Snapshot =
    Snapshot(totalGcCollections, totalGcTimeMs, heapUsedMb)
