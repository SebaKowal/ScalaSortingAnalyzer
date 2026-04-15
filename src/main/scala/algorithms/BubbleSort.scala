package algorithms

import model.SortStep
import model.SortStep.*

object BubbleSort extends SortAlgorithm:
  val name = "Bubble Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] = StepChannel.produce { emit =>
    val a = arr.clone()
    val n = a.length
    var swapped = true
    var end = n - 1
    while swapped do
      swapped = false
      for i <- 0 until end do
        emit(Compare(i, i + 1))
        if a(i) > a(i + 1) then
          val tmp = a(i); a(i) = a(i + 1); a(i + 1) = tmp
          emit(Swap(i, i + 1))
          swapped = true
      emit(MarkSorted(end))
      end -= 1
    emit(Done)
  }
