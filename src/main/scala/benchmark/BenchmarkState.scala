package benchmark

import scalafx.collections.ObservableBuffer

object BenchmarkState:
  val results: ObservableBuffer[AnyRef] = ObservableBuffer.empty[AnyRef]
