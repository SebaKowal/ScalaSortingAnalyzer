package benchmark.full

import model.ArrayGenerator

/** Measure loop for full benchmark mode.
 *  Wraps each sort invocation with before/after system snapshots.
 *  Timing accuracy is secondary — system metric accuracy is the goal. */
object Steps:

  type SortFn = Array[Int] => Unit

  /** Per-round record: wall-clock ns + system metric deltas. */
  case class RoundRecord(
                          wallNs:        Long,
                          heapDeltaMb:   Double,
                          gcCollections: Long,
                          gcPauseMs:     Long,
                          cpuTimeNs:     Long
                        )

  /** Run one measured round: snapshot → sort → snapshot → delta.
   *  Wall-clock time is still recorded but system probing adds noise to it. */
  def measureRound(
                    sortFn:     SortFn,
                    arr:        Array[Int],
                    probeHeap:  Boolean,
                    probeCpu:   Boolean,
                    probeGc:    Boolean
                  ): RoundRecord =
    val before = Snapshot.take(probeHeap, probeCpu, probeGc)
    val t0     = System.nanoTime()
    sortFn(arr)
    val t1     = System.nanoTime()
    val after  = Snapshot.take(probeHeap, probeCpu, probeGc)

    val (heapDelta, gcCount, gcTime, cpuDelta) = Snapshot.delta(before, after)
    RoundRecord(
      wallNs        = t1 - t0,
      heapDeltaMb   = heapDelta,
      gcCollections = gcCount,
      gcPauseMs     = gcTime,
      cpuTimeNs     = cpuDelta
    )

  /** Run all measure rounds and return per-round records. */
  def measureAll(
                  sortFn:       SortFn,
                  generator:    model.GeneratorType,
                  size:         Int,
                  rounds:       Int,
                  probeHeap:    Boolean,
                  probeCpu:     Boolean,
                  probeGc:      Boolean
                ): Array[RoundRecord] =
    // Pre-generate inputs before any probing begins
    val inputs = Array.tabulate(rounds)(_ => ArrayGenerator.generate(generator, size))
    Array.tabulate(rounds) { i =>
      measureRound(sortFn, inputs(i).clone(), probeHeap, probeCpu, probeGc)
    }