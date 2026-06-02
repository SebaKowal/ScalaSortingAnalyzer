package algorithms.vis

import algorithms.{SortAlgorithm, StepChannel}
import model.SortStep
import model.SortStep.*

object CountingSort extends SortAlgorithm:
  val name = "Counting Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] = StepChannel.produce { emit =>
    val a = arr.clone()
    val n = a.length

    if n == 0 then
      emit(Done)
    else
      val min = a.min
      val max = a.max
      val range = max - min + 1
      val count = Array.fill(range)(0)

      for i <- 0 until n do
        val v = a(i)
        emit(Compare(i, i))
        emit(CountIncrement(v))
        count(v - min) += 1
        emit(CountSet(v - min, count(v - min)))
      emit(ClearHighlights)

      for i <- 1 until range do
        emit(Compare(i - 1, i))
        count(i) += count(i - 1)
        emit(CountSet(i, count(i)))

      val output = Array.fill(n)(0)

      for i <- (0 until n).reverse do
        val v = a(i)
        emit(Compare(i, i))
        count(v - min) -= 1
        val pos = count(v - min)
        output(pos) = v

        emit(Set(pos, v))

      emit(MarkSortedRange(0, n - 1))
      emit(Done)
  }