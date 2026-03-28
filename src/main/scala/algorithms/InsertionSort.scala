package algorithms

import model.SortStep
import model.SortStep.*

object InsertionSort extends SortAlgorithm:
  val name = "Insertion Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] =
    val a = arr.clone()
    val buf = collection.mutable.ListBuffer.empty[SortStep]
    val n = a.length
    for i <- 1 until n do
      var j = i
      while j > 0 && { buf += Compare(j - 1, j); a(j - 1) > a(j) } do
        val tmp = a(j); a(j) = a(j - 1); a(j - 1) = tmp
        buf += Swap(j, j - 1)
        j -= 1
    // Only mark all green when fully sorted
    for i <- 0 until n do buf += MarkSorted(i)
    buf += Done
    buf.to(LazyList)