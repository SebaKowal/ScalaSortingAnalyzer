package benchmark.pure

/** Captures a single point-in-time nanoTime reading.
 *  Intentionally minimal — pure mode records nothing else. */
final class Snapshot private (val nanos: Long)

object Snapshot:
  /** Take a snapshot right now. Call immediately before/after the sort. */
  def take(): Snapshot = new Snapshot(System.nanoTime())

  /** Elapsed nanoseconds between two snapshots. */
  def elapsedNs(before: Snapshot, after: Snapshot): Long =
    after.nanos - before.nanos