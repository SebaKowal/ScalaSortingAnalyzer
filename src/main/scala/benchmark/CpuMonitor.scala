package benchmark

import java.lang.management.{ManagementFactory, ThreadMXBean}

/** Per-thread CPU time measurement using ThreadMXBean */
object CpuMonitor:

  private val threadMx: ThreadMXBean = ManagementFactory.getThreadMXBean

  private def isCpuTimeSupported: Boolean =
    threadMx.isThreadCpuTimeSupported

  def enableCpuTime(): Unit =
    if isCpuTimeSupported && !threadMx.isThreadCpuTimeEnabled then
      threadMx.setThreadCpuTimeEnabled(true)

  /** Returns (cpuTimeNs, userTimeNs) for current thread */
  def currentThreadTimes(): (Long, Long) =
    val tid = Thread.currentThread().threadId()
    val cpu  = if threadMx.isThreadCpuTimeEnabled then
      threadMx.getThreadCpuTime(tid).max(0)
    else 0L
    val user = if threadMx.isThreadCpuTimeEnabled then
      threadMx.getThreadUserTime(tid).max(0)
    else 0L
    (cpu, user)