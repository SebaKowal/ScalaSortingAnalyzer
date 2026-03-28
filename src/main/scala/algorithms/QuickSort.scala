package algorithms

import model.SortStep
import model.SortStep.*
import scala.util.Random

object QuickSort extends SortAlgorithm:
  val name = "Quick Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] =
    val a = arr.clone()
    val buf = collection.mutable.ListBuffer.empty[SortStep]
    quickSort(a, 0, a.length - 1, buf)
    // Mark all green only at the very end
    for i <- a.indices do buf += MarkSorted(i)
    buf += Done
    buf.to(LazyList)

  private def quickSort(a: Array[Int], lo: Int, hi: Int, buf: collection.mutable.ListBuffer[SortStep]): Unit =
    if lo < hi then
      val p = partition(a, lo, hi, buf)
      quickSort(a, lo, p - 1, buf)
      quickSort(a, p + 1, hi, buf)

  private def partition(a: Array[Int], lo: Int, hi: Int, buf: collection.mutable.ListBuffer[SortStep]): Int =
    val pivotIdx = lo + Random.nextInt(hi - lo + 1)
    val tmp = a(pivotIdx); a(pivotIdx) = a(hi); a(hi) = tmp
    buf += Swap(pivotIdx, hi)
    val pivot = a(hi)
    var i = lo - 1
    for j <- lo until hi do
      buf += Compare(j, hi)
      if a(j) <= pivot then
        i += 1
        if i != j then
          val t = a(i); a(i) = a(j); a(j) = t
          buf += Swap(i, j)
    val t = a(i + 1); a(i + 1) = a(hi); a(hi) = t
    buf += Swap(i + 1, hi)
    i + 1