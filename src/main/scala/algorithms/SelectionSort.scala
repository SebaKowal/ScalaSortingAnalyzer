package algorithms

import model.SortStep
import model.SortStep.*

object SelectionSort extends SortAlgorithm:
  val name = "Selection Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] =
    val a = arr.clone()
    val buf = collection.mutable.ListBuffer.empty[SortStep]
    val n = a.length
    for i <- 0 until n - 1 do
      var minIdx = i
      for j <- i + 1 until n do
        buf += Compare(minIdx, j)
        if a(j) < a(minIdx) then minIdx = j
      if minIdx != i then
        val tmp = a(i); a(i) = a(minIdx); a(minIdx) = tmp
        buf += Swap(i, minIdx)
      buf += MarkSorted(i)
    buf += MarkSorted(n - 1)
    buf += Done
    buf.to(LazyList)