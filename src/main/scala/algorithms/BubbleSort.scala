package algorithms

import model.SortStep
import model.SortStep.*

object BubbleSort extends SortAlgorithm:
  val name = "Bubble Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] =
    val a = arr.clone()
    val buf = collection.mutable.ListBuffer.empty[SortStep]
    val n = a.length
    var swapped = true
    var end = n - 1
    while swapped do
      swapped = false
      for i <- 0 until end do
        buf += Compare(i, i + 1)
        if a(i) > a(i + 1) then
          val tmp = a(i); a(i) = a(i + 1); a(i + 1) = tmp
          buf += Swap(i, i + 1)
          swapped = true
      buf += MarkSorted(end)
      end -= 1
    buf += Done
    buf.to(LazyList)