package algorithms.vis

import algorithms.{SortAlgorithm, StepChannel}
import model.SortStep
import model.SortStep.*

object CountingSort extends SortAlgorithm:
  val name = "Counting Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] = StepChannel.produce { emit =>
    val a = arr.clone()
    val n = a.length

    if n == 0 then emit(Done)
    else
      val max = a.max
      val min = a.min
      val range = max - min + 1

      // ── 1. Zliczanie wystąpień ──
      val count = Array.fill(range)(0)
      for i <- 0 until n do
        emit(Compare(i, i)) // Skanowanie tablicy
        count(a(i) - min) += 1

      // ── 2. Odtwarzanie tablicy za pomocą efektownych zamian (Swap) ──
      var outIdx = 0
      for v <- 0 until range do
        while count(v) > 0 do
          val targetValue = v + min

          // Szukamy, gdzie ten element AKTUALNIE znajduje się w tablicy (od outIdx w prawo)
          var sourceIdx = outIdx
          while sourceIdx < n && a(sourceIdx) != targetValue do
            sourceIdx += 1

          // Gdy go znajdziemy, zamiast robić Set, robimy fizyczny Swap
          if sourceIdx < n then
            if sourceIdx != outIdx then
              // Element jest dalej w tablicy -> zamieniamy go miejscami, żeby "przypłynął" na przód
              val tmp = a(outIdx)
              a(outIdx) = a(sourceIdx)
              a(sourceIdx) = tmp
              emit(Swap(outIdx, sourceIdx))
            else
              // Element już stoi na właściwym miejscu
              emit(Compare(outIdx, outIdx))

            // Oznaczamy element jako bezpieczny i posortowany
            emit(MarkSorted(outIdx))

          outIdx += 1
          count(v) -= 1

      emit(Done)
  }