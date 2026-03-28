package model

case class AlgorithmDetail(
                            algorithmType: AlgorithmType,
                            pseudocode: String,
                            scalaCode: String,
                            timeComplexityNotes: String,
                            spaceNotes: String,
                            prosAndCons: (List[String], List[String])  // (pros, cons)
                          )

object AlgorithmDetail:

  val all: Map[AlgorithmType, AlgorithmDetail] = Map(

    AlgorithmType.BubbleSort -> AlgorithmDetail(
      AlgorithmType.BubbleSort,
      pseudocode =
        """procedure bubbleSort(A: list of sortable items)
          |  n = length(A)
          |  repeat
          |    swapped = false
          |    for i = 1 to n - 1 do
          |      if A[i-1] > A[i] then
          |        swap(A[i-1], A[i])
          |        swapped = true
          |      end if
          |    end for
          |    n = n - 1
          |  until not swapped
          |end procedure""".stripMargin,
      scalaCode =
        """def bubbleSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone()
          |  var swapped = true
          |  var end = a.length - 1
          |  while swapped do
          |    swapped = false
          |    for i <- 0 until end do
          |      if a(i) > a(i + 1) then
          |        val tmp = a(i)
          |        a(i) = a(i + 1)
          |        a(i + 1) = tmp
          |        swapped = true
          |    end -= 1
          |  a""".stripMargin,
      timeComplexityNotes = "Each pass bubbles the largest unsorted element to its final position. Adaptive: terminates early if no swaps occur, giving O(n) on already-sorted input.",
      spaceNotes = "Operates in-place using only a constant amount of extra memory for the swap temporary variable.",
      prosAndCons = (
        List("Simple to implement and understand", "Adaptive — O(n) on nearly sorted data", "Stable sort", "In-place — O(1) space"),
        List("O(n²) average and worst case", "Very slow on large datasets", "High number of swaps compared to Selection Sort", "Rarely used in production")
      )
    ),

    AlgorithmType.SelectionSort -> AlgorithmDetail(
      AlgorithmType.SelectionSort,
      pseudocode =
        """procedure selectionSort(A: list of sortable items)
          |  n = length(A)
          |  for i = 0 to n - 2 do
          |    minIdx = i
          |    for j = i + 1 to n - 1 do
          |      if A[j] < A[minIdx] then
          |        minIdx = j
          |      end if
          |    end for
          |    if minIdx != i then
          |      swap(A[i], A[minIdx])
          |    end if
          |  end for
          |end procedure""".stripMargin,
      scalaCode =
        """def selectionSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone()
          |  val n = a.length
          |  for i <- 0 until n - 1 do
          |    var minIdx = i
          |    for j <- i + 1 until n do
          |      if a(j) < a(minIdx) then minIdx = j
          |    if minIdx != i then
          |      val tmp = a(i)
          |      a(i) = a(minIdx)
          |      a(minIdx) = tmp
          |  a""".stripMargin,
      timeComplexityNotes = "Always performs exactly n(n-1)/2 comparisons regardless of input order. The inner loop scans the entire unsorted portion to find the minimum each time.",
      spaceNotes = "In-place algorithm. Only needs a constant amount of extra space for index tracking and swap.",
      prosAndCons = (
        List("Minimizes number of swaps — O(n) swaps total", "Simple implementation", "In-place — O(1) space", "Performance is predictable regardless of input"),
        List("O(n²) always — no adaptivity", "Not stable", "Slower than insertion sort on nearly-sorted data", "Not suitable for large datasets")
      )
    ),

    AlgorithmType.InsertionSort -> AlgorithmDetail(
      AlgorithmType.InsertionSort,
      pseudocode =
        """procedure insertionSort(A: list of sortable items)
          |  for i = 1 to length(A) - 1 do
          |    key = A[i]
          |    j = i - 1
          |    while j >= 0 and A[j] > key do
          |      A[j + 1] = A[j]
          |      j = j - 1
          |    end while
          |    A[j + 1] = key
          |  end for
          |end procedure""".stripMargin,
      scalaCode =
        """def insertionSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone()
          |  for i <- 1 until a.length do
          |    var j = i
          |    while j > 0 && a(j - 1) > a(j) do
          |      val tmp = a(j)
          |      a(j) = a(j - 1)
          |      a(j - 1) = tmp
          |      j -= 1
          |  a""".stripMargin,
      timeComplexityNotes = "Builds the sorted array one element at a time. Excellent cache performance due to sequential memory access. Used as the base case in Timsort and Introsort.",
      spaceNotes = "In-place. The sorted portion grows leftward requiring no auxiliary array.",
      prosAndCons = (
        List("O(n) on nearly sorted data", "Stable sort", "Excellent for small arrays", "Online — can sort as data arrives", "Used inside hybrid sorts (Timsort)"),
        List("O(n²) worst and average case", "Many shifts for reverse-sorted input", "Not suitable for large random datasets")
      )
    ),

    AlgorithmType.MergeSort -> AlgorithmDetail(
      AlgorithmType.MergeSort,
      pseudocode =
        """procedure mergeSort(A, left, right)
          |  if left >= right then return
          |  mid = (left + right) / 2
          |  mergeSort(A, left, mid)
          |  mergeSort(A, mid + 1, right)
          |  merge(A, left, mid, right)
          |end procedure
          |
          |procedure merge(A, left, mid, right)
          |  L = A[left..mid]
          |  R = A[mid+1..right]
          |  i = 0, j = 0, k = left
          |  while i < len(L) and j < len(R) do
          |    if L[i] <= R[j] then A[k++] = L[i++]
          |    else A[k++] = R[j++]
          |  copy remaining elements
          |end procedure""".stripMargin,
      scalaCode =
        """def mergeSort(arr: Array[Int]): Array[Int] =
          |  def merge(l: Array[Int], r: Array[Int]): Array[Int] =
          |    val result = Array.ofDim[Int](l.length + r.length)
          |    var (i, j, k) = (0, 0, 0)
          |    while i < l.length && j < r.length do
          |      if l(i) <= r(j) then result(k) = l(i); i += 1
          |      else result(k) = r(j); j += 1
          |      k += 1
          |    l.drop(i) ++ r.drop(j) ++ result.take(k)
          |  def sort(a: Array[Int]): Array[Int] =
          |    if a.length <= 1 then a
          |    else
          |      val mid = a.length / 2
          |      merge(sort(a.take(mid)), sort(a.drop(mid)))
          |  sort(arr)""".stripMargin,
      timeComplexityNotes = "Divides the array in half recursively (log n levels), merging at each level in O(n). Guaranteed O(n log n) regardless of input — no worst case degradation.",
      spaceNotes = "Requires O(n) auxiliary space for the temporary arrays used during merging. This is the main drawback compared to in-place algorithms.",
      prosAndCons = (
        List("Guaranteed O(n log n) always", "Stable sort", "Excellent for linked lists", "Predictable performance", "Parallelizable"),
        List("O(n) extra space required", "Slower than quicksort in practice for arrays", "Not adaptive", "More complex implementation")
      )
    ),

    AlgorithmType.QuickSort -> AlgorithmDetail(
      AlgorithmType.QuickSort,
      pseudocode =
        """procedure quickSort(A, lo, hi)
          |  if lo < hi then
          |    p = partition(A, lo, hi)
          |    quickSort(A, lo, p - 1)
          |    quickSort(A, p + 1, hi)
          |end procedure
          |
          |procedure partition(A, lo, hi)
          |  pivot = A[hi]  (or random)
          |  i = lo - 1
          |  for j = lo to hi - 1 do
          |    if A[j] <= pivot then
          |      i = i + 1
          |      swap(A[i], A[j])
          |  swap(A[i+1], A[hi])
          |  return i + 1
          |end procedure""".stripMargin,
      scalaCode =
        """def quickSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone()
          |  def partition(lo: Int, hi: Int): Int =
          |    val pivotIdx = lo + Random.nextInt(hi - lo + 1)
          |    val tmp = a(pivotIdx); a(pivotIdx) = a(hi); a(hi) = tmp
          |    val pivot = a(hi); var i = lo - 1
          |    for j <- lo until hi do
          |      if a(j) <= pivot then
          |        i += 1
          |        val t = a(i); a(i) = a(j); a(j) = t
          |    val t = a(i+1); a(i+1) = a(hi); a(hi) = t
          |    i + 1
          |  def sort(lo: Int, hi: Int): Unit =
          |    if lo < hi then
          |      val p = partition(lo, hi)
          |      sort(lo, p - 1); sort(p + 1, hi)
          |  sort(0, a.length - 1); a""".stripMargin,
      timeComplexityNotes = "Randomized pivot selection avoids O(n²) worst case on sorted input. Average case is O(n log n) with very small constants — faster than merge sort in practice due to cache locality.",
      spaceNotes = "In-place partitioning uses O(log n) stack space for recursion. Worst case O(n) stack depth without randomization.",
      prosAndCons = (
        List("Fastest in practice for random data", "In-place — O(log n) space", "Excellent cache performance", "Widely used in standard libraries"),
        List("O(n²) worst case without randomization", "Not stable", "Recursive — stack overflow risk on huge arrays", "Poor on already-sorted data without random pivot")
      )
    ),

    AlgorithmType.HeapSort -> AlgorithmDetail(
      AlgorithmType.HeapSort,
      pseudocode =
        """procedure heapSort(A)
          |  buildMaxHeap(A)
          |  for i = length(A) - 1 downto 1 do
          |    swap(A[0], A[i])
          |    heapify(A, i, 0)
          |  end for
          |end procedure
          |
          |procedure heapify(A, size, i)
          |  largest = i
          |  l = 2*i + 1, r = 2*i + 2
          |  if l < size and A[l] > A[largest]: largest = l
          |  if r < size and A[r] > A[largest]: largest = r
          |  if largest != i then
          |    swap(A[i], A[largest])
          |    heapify(A, size, largest)
          |end procedure""".stripMargin,
      scalaCode =
        """def heapSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone(); val n = a.length
          |  def heapify(size: Int, i: Int): Unit =
          |    var largest = i
          |    val l = 2*i+1; val r = 2*i+2
          |    if l < size && a(l) > a(largest) then largest = l
          |    if r < size && a(r) > a(largest) then largest = r
          |    if largest != i then
          |      val tmp = a(i); a(i) = a(largest); a(largest) = tmp
          |      heapify(size, largest)
          |  for i <- n/2 - 1 to 0 by -1 do heapify(n, i)
          |  for end <- n-1 to 1 by -1 do
          |    val tmp = a(0); a(0) = a(end); a(end) = tmp
          |    heapify(end, 0)
          |  a""".stripMargin,
      timeComplexityNotes = "Building the heap takes O(n). Each of the n extractions takes O(log n) to restore the heap property. Total: O(n log n) guaranteed, with no worst-case degradation.",
      spaceNotes = "In-place. The heap is built within the original array. Only O(log n) stack space for recursive heapify calls.",
      prosAndCons = (
        List("Guaranteed O(n log n) always", "In-place — O(1) extra space", "No worst-case degradation", "Good for priority queue operations"),
        List("Not stable", "Poor cache performance", "Slower than quicksort in practice", "Complex implementation")
      )
    ),

    AlgorithmType.ShellSort -> AlgorithmDetail(
      AlgorithmType.ShellSort,
      pseudocode =
        """procedure shellSort(A)
          |  gap = length(A) / 2
          |  while gap > 0 do
          |    for i = gap to length(A) - 1 do
          |      temp = A[i]
          |      j = i
          |      while j >= gap and A[j - gap] > temp do
          |        A[j] = A[j - gap]
          |        j = j - gap
          |      end while
          |      A[j] = temp
          |    end for
          |    gap = gap / 2
          |  end while
          |end procedure""".stripMargin,
      scalaCode =
        """def shellSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone(); val n = a.length
          |  var gap = n / 2
          |  while gap > 0 do
          |    for i <- gap until n do
          |      val tmp = a(i); var j = i
          |      var continue = true
          |      while j >= gap && continue do
          |        if a(j - gap) > tmp then
          |          a(j) = a(j - gap); j -= gap
          |        else continue = false
          |      a(j) = tmp
          |    gap /= 2
          |  a""".stripMargin,
      timeComplexityNotes = "Performance depends heavily on the gap sequence. Knuth's sequence (1,4,13,40…) gives ~O(n^1.5). The simple n/2 halving used here gives O(n²) worst case but is fast in practice.",
      spaceNotes = "In-place. No auxiliary arrays needed. Uses O(1) extra space beyond the input.",
      prosAndCons = (
        List("Much faster than insertion sort", "In-place", "Simple to implement", "Good for medium-sized arrays"),
        List("Gap sequence choice is critical", "Not stable", "Complexity analysis is difficult", "Outperformed by O(n log n) algorithms on large data")
      )
    ),

    AlgorithmType.CocktailSort -> AlgorithmDetail(
      AlgorithmType.CocktailSort,
      pseudocode =
        """procedure cocktailSort(A)
          |  swapped = true
          |  start = 0, end = length(A) - 1
          |  while swapped do
          |    swapped = false
          |    for i = start to end - 1 do   // left → right
          |      if A[i] > A[i+1] then swap; swapped = true
          |    end = end - 1
          |    if not swapped then break
          |    swapped = false
          |    for i = end downto start + 1 do  // right → left
          |      if A[i] < A[i-1] then swap; swapped = true
          |    start = start + 1
          |  end while
          |end procedure""".stripMargin,
      scalaCode =
        """def cocktailSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone()
          |  var swapped = true
          |  var start = 0; var end = a.length - 1
          |  while swapped do
          |    swapped = false
          |    for i <- start until end do
          |      if a(i) > a(i+1) then
          |        val tmp = a(i); a(i) = a(i+1); a(i+1) = tmp
          |        swapped = true
          |    end -= 1
          |    if swapped then
          |      swapped = false
          |      for i <- end - 1 to start by -1 do
          |        if a(i) > a(i+1) then
          |          val tmp = a(i); a(i) = a(i+1); a(i+1) = tmp
          |          swapped = true
          |      start += 1
          |  a""".stripMargin,
      timeComplexityNotes = "Bidirectional bubble sort that handles 'turtles' (small values stuck at the end) better than standard bubble sort. Still O(n²) but with a lower constant in practice.",
      spaceNotes = "In-place. O(1) extra space. The bidirectional nature requires only boundary index tracking.",
      prosAndCons = (
        List("Better than bubble sort for 'turtle' values", "Stable sort", "Adaptive — early termination", "Simple to understand"),
        List("Still O(n²) complexity", "Marginal improvement over bubble sort", "Rarely used in practice", "More complex than bubble sort for little gain")
      )
    )
  )