package algorithms.pure

import scala.util.Random

/**
 * Pure sorting implementations for benchmarking.
 *
 * Design constraints (enforced, not aspirational):
 *   - No object allocation beyond the input array clone
 *   - No callbacks, no step tracking, no logging
 *   - No boxing — all primitives stay as Array[Int]
 *   - @inline on inner methods to eliminate call overhead
 *   - No recursion where iteration is possible
 *   - Tail-recursive where recursion is necessary
 *   - Each sort takes Array[Int] and sorts IN PLACE
 *   - Caller clones if they need the original preserved
 *
 * Benchmark validity:
 *   - BenchmarkRunner must clone the input before passing here
 *   - Each algorithm receives a fresh independent array
 *   - No shared mutable state between runs
 */
object PureSorting:

  // ── Swap helper — @inline eliminates the method call overhead ─
  @inline private def swap(a: Array[Int], i: Int, j: Int): Unit =
    val tmp = a(i); a(i) = a(j); a(j) = tmp

  // ── BubbleSort ────────────────────────────────────────────────
  // O(n²) worst/avg, O(n) best (adaptive via swapped flag)
  def bubbleSort(a: Array[Int]): Unit =
    val n = a.length
    var end = n - 1
    var swapped = true
    while swapped && end > 0 do
      swapped = false
      var i = 0
      while i < end do
        if a(i) > a(i + 1) then
          swap(a, i, i + 1)
          swapped = true
        i += 1
      end -= 1

  // ── SelectionSort ─────────────────────────────────────────────
  // O(n²) always, O(n) swaps — minimum possible swaps
  def selectionSort(a: Array[Int]): Unit =
    val n = a.length
    var i = 0
    while i < n - 1 do
      var minIdx = i
      var j = i + 1
      while j < n do
        if a(j) < a(minIdx) then minIdx = j
        j += 1
      if minIdx != i then swap(a, i, minIdx)
      i += 1

  // ── InsertionSort ─────────────────────────────────────────────
  // O(n²) worst, O(n) best — excellent cache locality, used in Timsort
  // Uses shift instead of swap: one write per shift vs three for swap
  def insertionSort(a: Array[Int]): Unit =
    val n = a.length
    var i = 1
    while i < n do
      val key = a(i)
      var j = i - 1
      // Shift elements right until correct position found
      while j >= 0 && a(j) > key do
        a(j + 1) = a(j)
        j -= 1
      a(j + 1) = key
      i += 1

  // ── InsertionSort on subrange — used by MergeSort/QuickSort hybrid ─
  @inline def insertionSortRange(a: Array[Int], lo: Int, hi: Int): Unit =
    var i = lo + 1
    while i <= hi do
      val key = a(i)
      var j = i - 1
      while j >= lo && a(j) > key do
        a(j + 1) = a(j)
        j -= 1
      a(j + 1) = key
      i += 1

  // ── MergeSort ─────────────────────────────────────────────────
  // O(n log n) guaranteed, stable
  // Uses a single pre-allocated auxiliary array — no per-merge allocation
  def mergeSort(a: Array[Int]): Unit =
    if a.length > 1 then
      val aux = new Array[Int](a.length)  // ONE allocation for the entire sort
      mergeSortRange(a, aux, 0, a.length - 1)

  private def mergeSortRange(a: Array[Int], aux: Array[Int], lo: Int, hi: Int): Unit =
    if hi - lo < 16 then
      // Insertion sort for small subarrays — avoids recursion overhead
      insertionSortRange(a, lo, hi)
    else
      val mid = lo + (hi - lo) / 2
      mergeSortRange(a, aux, lo, mid)
      mergeSortRange(a, aux, mid + 1, hi)
      // Skip merge if already in order — optimization for nearly-sorted data
      if a(mid) <= a(mid + 1) then return
      merge(a, aux, lo, mid, hi)

  private def merge(a: Array[Int], aux: Array[Int], lo: Int, mid: Int, hi: Int): Unit =
    // Copy to aux — System.arraycopy is a JVM intrinsic, much faster than element loop
    System.arraycopy(a, lo, aux, lo, hi - lo + 1)
    var i = lo
    var j = mid + 1
    var k = lo
    while k <= hi do
      if      i > mid          then { a(k) = aux(j); j += 1 }
      else if j > hi           then { a(k) = aux(i); i += 1 }
      else if aux(j) < aux(i)  then { a(k) = aux(j); j += 1 }
      else                          { a(k) = aux(i); i += 1 }
      k += 1

  // ── QuickSort ─────────────────────────────────────────────────
  // O(n log n) avg, O(n²) worst — randomized pivot eliminates worst case in practice
  // Iterative with explicit stack to avoid JVM stack overflow on large inputs
  private val threadLocalStack: ThreadLocal[Array[Int]] =
    ThreadLocal.withInitial(() => new Array[Int](200_002))  // max N=100k

  def quickSort(a: Array[Int]): Unit =
    if a.length > 1 then
      quickSortRange(a, 0, a.length - 1, threadLocalStack.get())

  private def quickSortRange(a: Array[Int], lo: Int, hi: Int, stack: Array[Int]): Unit =
    var top = -1
    top += 1;
    stack(top) = lo
    top += 1;
    stack(top) = hi

    while top >= 0 do
      val h = stack(top);
      top -= 1
      val l = stack(top);
      top -= 1

      if h - l < 24 then
        insertionSortRange(a, l, h)
      else
        val p = partitionHoare(a, l, h)

        if p > l then
          if (p - l) > (h - p) then
            top += 1;
            stack(top) = l;
            top += 1;
            stack(top) = p
            top += 1;
            stack(top) = p + 1;
            top += 1;
            stack(top) = h
          else
            top += 1;
            stack(top) = p + 1;
            top += 1;
            stack(top) = h
            top += 1;
            stack(top) = l;
            top += 1;
            stack(top) = p

  private def partitionHoare(a: Array[Int], lo: Int, hi: Int): Int =
    val mid = lo + (hi - lo) / 2

    // Median-of-three (inline dla wydajności)
    if a(mid) < a(lo) then swap(a, mid, lo)
    if a(hi) < a(lo) then swap(a, hi, lo)
    if a(mid) < a(hi) then swap(a, mid, hi)

    val pivot = a(mid) // Środkowy element jako piwot po sortowaniu trójki
    var i = lo - 1
    var j = hi + 1

    while true do
      i += 1
      while a(i) < pivot do i += 1
      j -= 1
      while a(j) > pivot do j -= 1

      if i >= j then return j
      swap(a, i, j)
    j

  private def partition(a: Array[Int], lo: Int, hi: Int): Int =
    // Median-of-three pivot — reduces worst case probability significantly
    val mid = lo + (hi - lo) / 2
    if a(mid) < a(lo)  then swap(a, mid, lo)
    if a(hi)  < a(lo)  then swap(a, hi,  lo)
    if a(mid) < a(hi)  then swap(a, mid, hi)
    // a(hi) is now the median — use as pivot
    val pivot = a(hi)
    var i = lo - 1
    var j = lo
    while j < hi do
      if a(j) <= pivot then
        i += 1
        swap(a, i, j)
      j += 1
    swap(a, i + 1, hi)
    i + 1

  // ── HeapSort ──────────────────────────────────────────────────
  // O(n log n) guaranteed, in-place, not stable
  // Iterative heapify — no recursion
  def heapSort(a: Array[Int]): Unit =
    val n = a.length
    // Build max-heap using Floyd's algorithm — O(n), better than naive O(n log n)
    var i = n / 2 - 1
    while i >= 0 do
      siftDown(a, i, n)
      i -= 1
    // Extract elements in order
    var end = n - 1
    while end > 0 do
      swap(a, 0, end)
      siftDown(a, 0, end)
      end -= 1

  @inline private def siftDown(a: Array[Int], root: Int, size: Int): Unit =
    var r = root
    var running = true
    while running do
      val left  = 2 * r + 1
      val right = 2 * r + 2
      var largest = r
      if left  < size && a(left)  > a(largest) then largest = left
      if right < size && a(right) > a(largest) then largest = right
      if largest != r then
        swap(a, r, largest)
        r = largest
      else
        running = false

  // ── ShellSort ─────────────────────────────────────────────────
  // O(n log²n) with Ciura gap sequence — much better than naive n/2 halving
  // The gap sequence matters enormously for performance
  def shellSort(a: Array[Int]): Unit =
    val n = a.length
    // Ciura's empirically optimal gap sequence (2001)
    // https://dx.doi.org/10.1016/S0020-0190(01)00182-0
    val gaps = Array(701, 301, 132, 57, 23, 10, 4, 1)
    var gi = 0
    // Skip gaps larger than array
    while gi < gaps.length && gaps(gi) >= n do gi += 1

    while gi < gaps.length do
      val gap = gaps(gi)
      var i = gap
      while i < n do
        val key = a(i)
        var j = i
        while j >= gap && a(j - gap) > key do
          a(j) = a(j - gap)
          j -= gap
        a(j) = key
        i += 1
      gi += 1

  // ── CocktailSort ─────────────────────────────────────────────
  // Bidirectional bubble sort — better for arrays with "turtles"
  def cocktailSort(a: Array[Int]): Unit =
    val n = a.length
    var swapped = true
    var start   = 0
    var end     = n - 1
    while swapped do
      swapped = false
      var i = start
      while i < end do
        if a(i) > a(i + 1) then
          swap(a, i, i + 1)
          swapped = true
        i += 1
      end -= 1
      if swapped then
        swapped = false
        var j = end
        while j > start do
          if a(j) < a(j - 1) then
            swap(a, j, j - 1)
            swapped = true
          j -= 1
        start += 1