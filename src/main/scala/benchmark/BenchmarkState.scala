package benchmark

import scalafx.collections.ObservableBuffer

object BenchmarkState:
  val results: ObservableBuffer[BenchmarkResult] = ObservableBuffer.empty[BenchmarkResult]
