package benchmark.pure

import model.ArrayGenerator

/** Warmup and measure loop logic for pure benchmark mode.
 *  Separated from the engine so each concern is independently testable. */
object Steps:

  type SortFn = Array[Int] => Unit

  /** Warmup phase — runs sortFn the given number of times, results discarded.
   *  Purpose: trigger JIT compilation to C2 before measurement begins.
   *  Uses pre-generated inputs to avoid ArrayGenerator overhead inside the loop. */
  def warmup(
              sortFn:       SortFn,
              generator:    model.GeneratorType,
              size:         Int,
              rounds:       Int
            ): Unit =
    // Pre-generate all warmup inputs once — avoids generator overhead per round
    val inputs = Array.tabulate(rounds)(_ => ArrayGenerator.generate(generator, size))
    var i = 0
    while i < rounds do
      sortFn(inputs(i).clone())
      i += 1
    // Allow GC to collect warmup arrays before measurement
    System.gc()
    Thread.sleep(50)

  /** Measure phase — times each sort run, returns raw nanosecond samples.
   *  HARD CONSTRAINT: only sortFn(arr) sits between the two nanoTime() calls.
   *  Nothing else — no logging, no probing, no branching — inside the window. */
  def measure(
               sortFn:    SortFn,
               generator: model.GeneratorType,
               size:      Int,
               rounds:    Int
             ): Array[Long] =
    // Pre-generate all measurement inputs before timing begins
    val inputs  = Array.tabulate(rounds)(_ => ArrayGenerator.generate(generator, size))
    val samples = new Array[Long](rounds)
    var i = 0
    while i < rounds do
      val arr = inputs(i).clone()
      // ══ TIMING WINDOW START ══════════════════════════════
      val t0 = System.nanoTime()
      sortFn(arr)
      val t1 = System.nanoTime()
      // ══ TIMING WINDOW END ════════════════════════════════
      samples(i) = t1 - t0
      i += 1
    samples