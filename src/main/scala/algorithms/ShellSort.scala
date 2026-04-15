package algorithms

import model.SortStep
import model.SortStep.*

object ShellSort extends SortAlgorithm:
  val name = "Shell Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] = StepChannel.produce { emit =>
    val a = arr.clone()
    val n = a.length
    var gap = n / 2

    while gap > 0 do
      for i <- gap until n do
        val tmp = a(i)
        var j = i
        var continue = true
        while j >= gap && continue do
          emit(Compare(j - gap, j))
          if a(j - gap) > tmp then
            a(j) = a(j - gap)
            emit(Set(j, a(j - gap)))
            j -= gap
          else
            continue = false
        if j != i then
          a(j) = tmp
          emit(Set(j, tmp))
      gap /= 2

    for i <- a.indices do emit(MarkSorted(i))
    emit(Done)
  }
