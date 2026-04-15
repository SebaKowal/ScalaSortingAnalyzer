package algorithms

import model.SortStep
import model.SortStep.*

import scala.annotation.tailrec

object HeapSort extends SortAlgorithm:
  val name = "Heap Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] = StepChannel.produce { emit =>
    val a = arr.clone()
    val n = a.length

    for i <- (n / 2 - 1) to 0 by -1 do
      heapify(a, n, i, emit)

    for end <- (n - 1) to 1 by -1 do
      val tmp = a(0); a(0) = a(end); a(end) = tmp
      emit(Swap(0, end))
      heapify(a, end, 0, emit)
      emit(MarkSorted(end))

    if n > 0 then emit(MarkSorted(0))
    emit(Done)
  }

  @tailrec
  private def heapify(a: Array[Int], heapSize: Int, i: Int, emit: SortStep => Unit): Unit =
    var largest = i
    val l = 2 * i + 1
    val r = 2 * i + 2

    if l < heapSize then
      emit(Compare(l, largest))
      if a(l) > a(largest) then largest = l

    if r < heapSize then
      emit(Compare(r, largest))
      if a(r) > a(largest) then largest = r

    if largest != i then
      val tmp = a(i); a(i) = a(largest); a(largest) = tmp
      emit(Swap(i, largest))
      heapify(a, heapSize, largest, emit)
