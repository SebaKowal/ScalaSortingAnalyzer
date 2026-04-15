package algorithms

import model.SortStep
import model.SortStep.*

object InsertionSort extends SortAlgorithm:
  val name = "Insertion Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] = StepChannel.produce { emit =>
    val a = arr.clone()
    val n = a.length
    for i <- 1 until n do
      var j = i
      while j > 0 && { emit(Compare(j - 1, j)); a(j - 1) > a(j) } do
        val tmp = a(j); a(j) = a(j - 1); a(j - 1) = tmp
        emit(Swap(j, j - 1))
        j -= 1
    for i <- 0 until n do emit(MarkSorted(i))
    emit(Done)
  }
