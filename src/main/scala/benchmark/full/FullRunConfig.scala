package benchmark.full

import benchmark.pure.PureRunConfig
import model.GeneratorType
import ui.utils.AlgorithmType

/** Configuration for a full benchmark run.
 *  Extends PureRunConfig with optional system probe toggles.
 *  Each probe can be disabled independently without switching modes. */
case class FullRunConfig(
                          // ── Inherited from pure ───────────────────────────────
                          algo:          AlgorithmType,
                          generator:     GeneratorType,
                          size:          Int,
                          warmupRounds:  Int     = 2000,
                          measureRounds: Int     = 50,
                          // ── System probe toggles ──────────────────────────────
                          probeHeap:     Boolean = true,   // heap delta MB + alloc rate
                          probeCpu:      Boolean = true,   // CPU thread time via ThreadMXBean
                          probeGc:       Boolean = true    // GC collection count + pause ms
                        ):
  /** Convert to a PureRunConfig for delegation to PureBenchmarkEngine. */
  def toPureConfig: PureRunConfig =
    PureRunConfig(algo, generator, size, warmupRounds, measureRounds)