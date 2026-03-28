package algorithms

import model.SortStep
import model.SortStep.*

object MergeSort extends SortAlgorithm:
  val name = "Merge Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] =
    val a = arr.clone()
    val buf = collection.mutable.ListBuffer.empty[SortStep]
    mergeSort(a, 0, a.length - 1, buf)
    for i <- a.indices do buf += MarkSorted(i)
    buf += Done
    buf.to(LazyList)

  private def mergeSort(a: Array[Int], l: Int, r: Int, buf: collection.mutable.ListBuffer[SortStep]): Unit =
    if l < r then
      val mid = (l + r) / 2
      mergeSort(a, l, mid, buf)
      mergeSort(a, mid + 1, r, buf)
      merge(a, l, mid, r, buf)

  private def merge(a: Array[Int], l: Int, mid: Int, r: Int, buf: collection.mutable.ListBuffer[SortStep]): Unit =
    val left  = a.slice(l, mid + 1)
    val right = a.slice(mid + 1, r + 1)
    var i = 0; var j = 0; var k = l
    while i < left.length && j < right.length do
      buf += Compare(l + i, mid + 1 + j)
      if left(i) <= right(j) then
        a(k) = left(i)
        buf += Set(k, left(i))
        i += 1
      else
        a(k) = right(j)
        buf += Set(k, right(j))
        j += 1
      k += 1
    while i < left.length do
      a(k) = left(i)
      buf += Set(k, left(i))
      i += 1; k += 1
    while j < right.length do
      a(k) = right(j)
      buf += Set(k, right(j))
      j += 1; k += 1