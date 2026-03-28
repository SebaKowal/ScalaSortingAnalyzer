package benchmark

case class BenchmarkResult(
                            algorithmName: String,
                            arraySize: Int,
                            generatorType: String,
                            comparisons: Long,
                            swaps: Long,
                            elapsedMs: Long,
                            memoryUsedMb: Double,
                            gcCollections: Long,
                            gcTimeMs: Long,
                            cpuLoadPercent: Double
                          )