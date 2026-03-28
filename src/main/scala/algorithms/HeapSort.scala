package algorithms

import model.SortStep
import model.SortStep.*

import scala.annotation.tailrec

object HeapSort extends SortAlgorithm:
  val name = "Heap Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] =
    val a = arr.clone()
    val buf = collection.mutable.ListBuffer.empty[SortStep]
    val n = a.length

    // Build max-heap — only iterate valid parent indices
    for i <- (n / 2 - 1) to 0 by -1 do
      heapify(a, n, i, buf)

    // Extract elements one by one
    for end <- (n - 1) to 1 by -1 do
      val tmp = a(0); a(0) = a(end); a(end) = tmp
      buf += Swap(0, end)
      heapify(a, end, 0, buf)   // heap size shrinks: pass `end` not `n`

    for i <- a.indices do buf += MarkSorted(i)
    buf += Done
    buf.to(LazyList)

  @tailrec
  private def heapify(a: Array[Int], heapSize: Int, i: Int, buf: collection.mutable.ListBuffer[SortStep]): Unit =
    var largest = i
    val l = 2 * i + 1
    val r = 2 * i + 2

    if l < heapSize then
      buf += Compare(l, largest)
      if a(l) > a(largest) then largest = l

    if r < heapSize then
      buf += Compare(r, largest)
      if a(r) > a(largest) then largest = r

    if largest != i then
      val tmp = a(i); a(i) = a(largest); a(largest) = tmp
      buf += Swap(i, largest)
      heapify(a, heapSize, largest, buf)