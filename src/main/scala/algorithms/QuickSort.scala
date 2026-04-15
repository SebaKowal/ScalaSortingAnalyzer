package algorithms

import model.SortStep
import model.SortStep.*
import scala.util.Random

object QuickSort extends SortAlgorithm:
  val name = "Quick Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] = StepChannel.produce { emit =>
    val a = arr.clone()
    quickSort(a, 0, a.length - 1, emit)
    for i <- a.indices do emit(MarkSorted(i))
    emit(Done)
  }

  private def quickSort(a: Array[Int], lo: Int, hi: Int, emit: SortStep => Unit): Unit =
    if lo < hi then
      val p = partition(a, lo, hi, emit)
      quickSort(a, lo, p - 1, emit)
      quickSort(a, p + 1, hi, emit)

  private def partition(a: Array[Int], lo: Int, hi: Int, emit: SortStep => Unit): Int =
    val pivotIdx = lo + Random.nextInt(hi - lo + 1)
    val tmp = a(pivotIdx); a(pivotIdx) = a(hi); a(hi) = tmp
    emit(Swap(pivotIdx, hi))
    val pivot = a(hi)
    var i = lo - 1
    for j <- lo until hi do
      emit(Compare(j, hi))
      if a(j) <= pivot then
        i += 1
        if i != j then
          val t = a(i); a(i) = a(j); a(j) = t
          emit(Swap(i, j))
    val t = a(i + 1); a(i + 1) = a(hi); a(hi) = t
    emit(Swap(i + 1, hi))
    i + 1
