package algorithms

import model.SortStep
import model.SortStep.*

object SelectionSort extends SortAlgorithm:
  val name = "Selection Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] = StepChannel.produce { emit =>
    val a = arr.clone()
    val n = a.length
    for i <- 0 until n - 1 do
      var minIdx = i
      for j <- i + 1 until n do
        emit(Compare(minIdx, j))
        if a(j) < a(minIdx) then minIdx = j
      if minIdx != i then
        val tmp = a(i); a(i) = a(minIdx); a(minIdx) = tmp
        emit(Swap(i, minIdx))
      emit(MarkSorted(i))
    emit(MarkSorted(n - 1))
    emit(Done)
  }
