package benchmark.pure

import ui.utils.AlgorithmType
import model.GeneratorType

/** Configuration for a pure (timing-only) benchmark run.
 *  Kept minimal — only what the timing engine needs. */
case class PureRunConfig(
                          algo:          AlgorithmType,
                          generator:     GeneratorType,
                          size:          Int,
                          warmupRounds:  Int = 2000,   // JIT warmup — results discarded
                          measureRounds: Int = 50      // actual timed rounds
                        )