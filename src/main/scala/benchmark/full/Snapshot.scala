package benchmark.full

import java.lang.management.ManagementFactory
import scala.jdk.CollectionConverters.*

/** Point-in-time snapshot of system metrics for full benchmark mode.
 *  Captured before and after each measured round — deltas give per-round cost. */
final class Snapshot private(
                              val heapMb:        Double,
                              val gcCollections: Long,
                              val gcPauseMs:     Long,
                              val cpuTimeNs:     Long
                            )

object Snapshot:
  private val memBean  = ManagementFactory.getMemoryMXBean
  private val gcBeans  = ManagementFactory.getGarbageCollectorMXBeans.asScala.toList
  private val threadMx = ManagementFactory.getThreadMXBean

  /** Enable CPU time tracking if supported — call once at startup. */
  def enableCpuTime(): Unit =
    if threadMx.isThreadCpuTimeSupported && !threadMx.isThreadCpuTimeEnabled then
      threadMx.setThreadCpuTimeEnabled(true)

  /** Take a snapshot of current system state. */
  def take(probeHeap: Boolean, probeCpu: Boolean, probeGc: Boolean): Snapshot =
    val heap = if probeHeap then
      memBean.getHeapMemoryUsage.getUsed.toDouble / (1024.0 * 1024.0)
    else 0.0

    val (gcCount, gcTime) = if probeGc then
      val count = gcBeans.map(_.getCollectionCount).filter(_ >= 0).sum
      val time  = gcBeans.map(_.getCollectionTime).filter(_ >= 0).sum
      (count, time)
    else (0L, 0L)

    val cpu = if probeCpu && threadMx.isThreadCpuTimeEnabled then
      threadMx.getCurrentThreadCpuTime.max(0L)
    else 0L

    new Snapshot(heap, gcCount, gcTime, cpu)

  /** Compute deltas between two snapshots. All values are >= 0. */
  def delta(before: Snapshot, after: Snapshot): (Double, Long, Long, Long) =
    val heapDelta = (after.heapMb        - before.heapMb).max(0.0)
    val gcDelta   = (after.gcCollections - before.gcCollections).max(0L)
    val gcTime    = (after.gcPauseMs     - before.gcPauseMs).max(0L)
    val cpuDelta  = (after.cpuTimeNs     - before.cpuTimeNs).max(0L)
    (heapDelta, gcDelta, gcTime, cpuDelta)