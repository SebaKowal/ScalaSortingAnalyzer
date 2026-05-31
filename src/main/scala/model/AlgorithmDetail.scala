package model

case class AlgorithmDetail(
                            algorithmType: AlgorithmType,
                            pseudocode: String,
                            scalaCodeImperative: String,
                            scalaCodeFunctional: String,
                            timeComplexityNotes: String,
                            spaceNotes: String,
                            prosAndCons: (List[String], List[String])
                          )

object AlgorithmDetail:

  val all: Map[AlgorithmType, AlgorithmDetail] = Map(

    AlgorithmType.BubbleSort -> AlgorithmDetail(
      AlgorithmType.BubbleSort,

      pseudocode =
        """BUBBLESORT(A)
          |1  dla i = 1 do A.length - 1
          |2      dla j = A.length w dół do i + 1
          |3          jeśli A[j] < A[j - 1]
          |4              zamień A[j] z A[j - 1]""".stripMargin,

      scalaCodeImperative =
        """def bubbleSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone()
          |  var swapped = true
          |  var end = a.length - 1
          |
          |  while swapped do
          |    swapped = false
          |
          |    for i <- 0 until end do
          |      if a(i) > a(i + 1) then
          |        val tmp = a(i)
          |        a(i) = a(i + 1)
          |        a(i + 1) = tmp
          |        swapped = true
          |
          |    end -= 1
          |
          |  a""".stripMargin,

      scalaCodeFunctional =
        """def bubbleSort(list: List[Int]): List[Int] =
          |  def pass(xs: List[Int]): (List[Int], Boolean) = xs match
          |    case a :: b :: tail if a > b =>
          |      val (rest, swapped) = pass(a :: tail)
          |      (b :: rest, swapped || true)
          |
          |    case a :: b :: tail =>
          |      val (rest, swapped) = pass(b :: tail)
          |      (a :: rest, swapped)
          |
          |    case xs => (xs, false)
          |
          |  def loop(xs: List[Int]): List[Int] =
          |    val (next, swapped) = pass(xs)
          |    if swapped then loop(next) else next
          |
          |  loop(list)""".stripMargin,

      timeComplexityNotes =
        "Bubble Sort has a worst-case and average time complexity of O(n²). In the best case, when the data is already sorted and early termination is applied, it can run in O(n).",

      spaceNotes =
        "The algorithm operates in-place and uses O(1) additional memory.",

      prosAndCons = (
        List(
          "Very simple implementation",
          "Good local memory usage — operates on adjacent elements",
          "Easy to analyse and visualise"
        ),
        List(
          "Worst-case and average complexity of O(n²)",
          "Can perform a large number of unnecessary comparisons and swaps",
          "Very poor practical performance — rarely used in real-world systems"
        )
      )
    ),

    AlgorithmType.QuickSort -> AlgorithmDetail(
      AlgorithmType.QuickSort,
      pseudocode =
        """procedura quickSort(A, p, r)
          |  jeśli p < r to
          |    q = partycja(A, p, r)
          |    quickSort(A, p, q - 1)
          |    quickSort(A, q + 1, r)
          |
          |
          |procedura partycja(A, p, r)
          |  x = A[r]
          |  i = p - 1
          |  dla j = p do r - 1 wykonuj
          |    jeśli A[j] <= x wtedy
          |      i = i + 1
          |      zamień(A[i], A[j])
          |  zamień(A[i + 1], A[r])
          |  zwróć i + 1
          """.stripMargin,

      scalaCodeImperative =
        """def quickSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone()
          |
          |  def partition(lo: Int, hi: Int): Int =
          |    val pivot = a(hi)
          |    var i = lo - 1
          |    for j <- lo until hi do
          |      if a(j) <= pivot then
          |        i += 1
          |        val tmp = a(i); a(i) = a(j); a(j) = tmp
          |    val tmp = a(i + 1); a(i + 1) = a(hi); a(hi) = tmp
          |    i + 1
          |
          |  def sort(lo: Int, hi: Int): Unit =
          |    if lo < hi then
          |      val p = partition(lo, hi)
          |      sort(lo, p - 1)
          |      sort(p + 1, hi)
          |
          |  sort(0, a.length - 1)
          |  a""".stripMargin,

      scalaCodeFunctional =
        """def sort[T](list: List[T])(implicit ord: Ordering[T]): List[T] =
          |
          |  def quicksort(xs: List[T]): List[T] = xs match {
          |    case Nil => Nil
          |    case pivot :: tail =>
          |      val (less, equal, greater) =
          |        xs.foldLeft((List.empty[T], List.empty[T], List.empty[T])) {
          |          case ((l, e, g), x) =>
          |            if (ord.lt(x, pivot)) (x :: l, e, g)
          |            else if (ord.equiv(x, pivot)) (l, x :: e, g)
          |            else (l, e, x :: g)
          |        }
          |      quicksort(less) ::: equal ::: quicksort(greater)
          |  }
          |
          |  quicksort(list)""".stripMargin,

      timeComplexityNotes =
        "Average time complexity is O(n log n). In the worst case, with a poor pivot choice, complexity degrades to O(n²), which is why randomisation or median-of-three pivot selection is commonly used.",

      spaceNotes =
        "Sorts in-place. Requires O(log n) stack space on average due to recursion, and O(n) in the worst case.",

      prosAndCons = (
        List(
          "Very fast in practice — average O(n log n)",
          "Good cache performance (cache-friendly)",
          "Small constant factors in asymptotic complexity",
          "Widely used in standard libraries"
        ),
        List(
          "Worst-case complexity of O(n²)",
          "Not stable",
          "Performance depends heavily on pivot selection",
          "Risk of stack overflow with deep recursion"
        )
      )
    ),

    AlgorithmType.InsertionSort -> AlgorithmDetail(
      AlgorithmType.InsertionSort,

      pseudocode =
        """INSERTION-SORT(A)
          |1  dla j = 2 do A.length
          |2      klucz = A[j]
          |3      // Wstawienie elementu A[j] do posortowanej sekwencji A[1..j - 1]
          |4      i = j - 1
          |5      dopóki i > 0 oraz A[i] > klucz
          |6          A[i + 1] = A[i]
          |7          i = i - 1
          |8      A[i + 1] = klucz""".stripMargin,

      scalaCodeImperative =
        """def insertionSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone()
          |
          |  for j <- 1 until a.length do
          |    val key = a(j)
          |    var i = j - 1
          |
          |    while i >= 0 && a(i) > key do
          |      a(i + 1) = a(i)
          |      i -= 1
          |
          |    a(i + 1) = key
          |
          |  a""".stripMargin,

      scalaCodeFunctional =
        """def insertionSort(list: List[Int]): List[Int] =
          |
          |  def insert(current: Int, sorted: List[Int]): List[Int] = sorted match {
          |    case head :: tail if current > head =>
          |      head :: insert(current, tail)
          |
          |    case _ =>
          |      current :: sorted
          |  }
          |
          |  def sort(source: List[Int], result: List[Int]): List[Int] = source match {
          |    case head :: tail =>
          |      sort(tail, insert(head, result))
          |    case Nil =>
          |      result
          |  }
          |
          |  sort(list, Nil)""".stripMargin,

      timeComplexityNotes =
        "Insertion Sort achieves O(n) in the best case when the data is already partially or fully sorted. In the average and worst case it runs in O(n²).",

      spaceNotes =
        "The algorithm operates in-place and requires only O(1) additional memory.",

      prosAndCons = (
        List(
          "Very fast for nearly sorted data — O(n)",
          "Stable sorting algorithm",
          "Simple implementation",
          "In-place — O(1) memory usage"
        ),
        List(
          "Worst-case and average time complexity of O(n²)",
          "Requires many element shifts",
          "Scales poorly for large datasets",
          "Sensitive to reverse-sorted input"
        )
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
          |procedure buildMaxHeap(A)
          |  heap-size = length(A)
          |  for i = floor(length(A)/2) downto 0 do
          |    heapify(A, heap-size, i)
          |  end for
          |end procedure
          |
          |procedure heapify(A, size, i)
          |  largest = i
          |  l = 2*i + 1
          |  r = 2*i + 2
          |  if l < size and A[l] > A[largest]: largest = l
          |  if r < size and A[r] > A[largest]: largest = r
          |  if largest != i then
          |    swap(A[i], A[largest])
          |    heapify(A, size, largest)
          |end procedure""".stripMargin,

      scalaCodeImperative =
        """def heapSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone(); val n = a.length
          |
          |  def heapify(size: Int, i: Int): Unit =
          |    var largest = i
          |    val l = 2*i+1; val r = 2*i+2
          |    if l < size && a(l) > a(largest) then largest = l
          |    if r < size && a(r) > a(largest) then largest = r
          |    if largest != i then
          |      val tmp = a(i); a(i) = a(largest); a(largest) = tmp
          |      heapify(size, largest)
          |
          |  for i <- n/2 - 1 to 0 by -1 do heapify(n, i)
          |  for end <- n-1 to 1 by -1 do
          |    val tmp = a(0); a(0) = a(end); a(end) = tmp
          |    heapify(end, 0)
          |
          |  a""".stripMargin,

      scalaCodeFunctional =
        """ def sort[T](array: Array[T])(implicit ord: Ordering[T]): Array[T] =
          |  val a = array.clone()
          |  val n = a.length
          |
          |  def swap(i: Int, j: Int): Unit =
          |    val tmp = a(i); a(i) = a(j); a(j) = tmp
          |
          |  def leftChild(i: Int): Option[Int] =
          |    Some(2 * i + 1).filter(_ < n)  // uwaga: size przekazywane przy heapify
          |
          |  @tailrec
          |  def heapify(size: Int, i: Int): Unit =
          |    val largest =
          |      Seq(Some(i),
          |          Some(2 * i + 1).filter(_ < size),
          |          Some(2 * i + 2).filter(_ < size))
          |        .flatten
          |        .maxBy(a(_))
          |    if largest != i then
          |      swap(i, largest)
          |      heapify(size, largest)
          |
          |  @tailrec
          |  def buildHeap(i: Int): Unit =
          |    if i >= 0 then
          |      heapify(n, i)
          |      buildHeap(i - 1)
          |
          |  @tailrec
          |  def extract(end: Int): Unit =
          |    if end >= 1 then
          |      swap(0, end)
          |      heapify(end, 0)
          |      extract(end - 1)
          |
          |  buildHeap(n / 2 - 1)
          |  extract(n - 1)
          |  a""".stripMargin,

      timeComplexityNotes =
        "Building the heap takes O(n). Each element extraction requires O(log n), giving an overall time complexity of O(n log n) in all cases — best, average, and worst.",

      spaceNotes =
        "Sorts in-place using O(1) additional memory. The recursive call stack for the heapify procedure has O(log n) depth.",

      prosAndCons = (
        List(
          "Guaranteed O(n log n) in all cases",
          "In-place sorting — O(1) additional memory",
          "No degradation to O(n²)",
          "Well suited for priority queue structures"
        ),
        List(
          "Not a stable sort",
          "Poor cache locality — frequent cache misses",
          "In practice slower than Quick Sort",
          "More complex implementation"
        )
      )
    ),


    AlgorithmType.CountingSort -> AlgorithmDetail(
      AlgorithmType.CountingSort,
      pseudocode =
        """COUNTING-SORT(A, B, k)
          |1   niech C[0..k] będzie nową tablicą
          |2   dla i = 0 do k
          |3       C[i] = 0
          |4   dla j = 1 do A.długość
          |5       C[A[j]] = C[A[j]] + 1
          |6   // C[i] zawiera teraz liczbę elementów równych i
          |7   dla i = 1 do k
          |8       C[i] = C[i] + C[i − 1]
          |9   // C[i] zawiera teraz liczbę elementów mniejszych lub równych i
          |10  dla j = A.długość do 1 (malejąco)
          |11      B[C[A[j]]] = A[j]
          |12      C[A[j]] = C[A[j]] − 1
          |end procedure""".stripMargin,

      scalaCodeImperative =
        """def countingSort(arr: Array[Int]): Array[Int] =
          |  val a = arr.clone()
          |  if a.length <= 1 then return a
          |
          |  val min = a.min
          |  val max = a.max
          |  val count = Array.fill(max - min + 1)(0)
          |  val output = new Array[Int](a.length)
          |
          |  for x <- a do
          |    count(x - min) += 1
          |
          |  for i <- 1 to count.length - 1 do
          |    count(i) += count(i - 1)
          |
          |  var i = a.length - 1
          |  while i >= 0 do
          |    val x = a(i)
          |    val pos = count(x - min) - 1
          |    output(pos) = x
          |    count(x - min) -= 1
          |    i -= 1
          |
          |  Array.copy(output, 0, a, 0, a.length)
          |  a""".stripMargin,

      scalaCodeFunctional =
        """ def countingSort(arr: Array[Int]): Array[Int] =
          |  if arr.length <= 1 then arr.clone()
          |  else
          |    val min = arr.min
          |    val max = arr.max
          |    val count  = new Array[Int](max - min + 1)
          |    val output = new Array[Int](arr.length)
          |
          |    @tailrec
          |    def buildCount(i: Int): Unit =
          |      if i < arr.length then
          |        count(arr(i) - min) += 1
          |        buildCount(i + 1)
          |
          |    @tailrec
          |    def buildPrefix(i: Int): Unit =
          |      if i < count.length then
          |        count(i) += count(i - 1)
          |        buildPrefix(i + 1)
          |
          |    @tailrec
          |    def placeElements(i: Int): Unit =
          |      if i >= 0 then
          |        val x   = arr(i)
          |        val pos = count(x - min) - 1
          |        output(pos) = x
          |        count(x - min) -= 1
          |        placeElements(i - 1)
          |
          |    buildCount(0)
          |    buildPrefix(1)
          |    placeElements(arr.length - 1)
          |    output""".stripMargin,

      timeComplexityNotes =
        "Linear time complexity O(n + k); no element comparisons are performed; highly efficient when the value range k is small relative to n.",

      spaceNotes =
        "Requires knowledge of the value range in advance; uses O(n + k) additional memory for the count and output arrays; restricted to integer data with a limited value range.",

      prosAndCons = (
        List(
          "Linear time complexity O(n + k)",
          "Stable sorting algorithm",
          "No element comparisons required",
          "Very efficient for small value ranges"
        ),
        List(
          "Requires prior knowledge of the value range",
          "Additional memory consumption",
          "Restricted to integer data with a limited range of values",
          "Less universal than comparison-based algorithms"
        )
      )
    ),

    AlgorithmType.BucketSort -> AlgorithmDetail(
      AlgorithmType.BucketSort,
      pseudocode =
        """BUCKET-SORT(A)
          |1   n = A.długość
          |2   niech B[0..n−1] będzie nową tablicą
          |3   dla i = 0 do n−1
          |4       utwórz B[i] jako pustą listę
          |5   dla i = 1 do n
          |6       wstaw A[i] do listy B[⌊n · A[i]⌋]
          |7   dla i = 0 do n−1
          |8       posortuj listę B[i] sortowaniem np. przez wstawianie
          |9   połącz listy B[0], B[1], ..., B[n−1] w kolejności
          |end procedura""".stripMargin,

      scalaCodeImperative =
        """def bucketSort(arr: Array[Int], numberOfBuckets: Int): Array[Int] =
          |  if arr.isEmpty then arr.clone()
          |  else
          |    val a   = arr.clone()
          |    val min = a.min
          |    val max = a.max
          |    val bucketInterval =
          |      Math.max(1, (max - min + 1) / numberOfBuckets)
          |    val buckets =
          |      Array.fill(numberOfBuckets)(scala.collection.mutable.ArrayBuffer.empty[Int])
          |
          |    for x <- a do
          |      val idx = Math.min((x - min) / bucketInterval, numberOfBuckets - 1)
          |      buckets(idx) += x
          |
          |    var i = 0
          |    for b <- buckets do
          |      val sorted = b.sorted
          |      for x <- sorted do
          |        a(i) = x
          |        i += 1
          |
          |    a""".stripMargin,

      scalaCodeFunctional =
        """def bucketSort(arr: Array[Int], numberOfBuckets: Int): Array[Int] =
          |  if arr.isEmpty then arr.clone()
          |  else
          |    val min = arr.min
          |    val max = arr.max
          |    val bucketInterval =
          |      Math.max(1, (max - min + 1) / numberOfBuckets)
          |
          |    arr
          |      .foldLeft(Vector.fill(numberOfBuckets)(Vector.empty[Int])) { (b, x) =>
          |        val idx = Math.min((x - min) / bucketInterval, numberOfBuckets - 1)
          |        b.updated(idx, b(idx) :+ x)
          |      }
          |      .flatMap(_.sorted)
          |      .toArray""".stripMargin,

      timeComplexityNotes =
        "Expected time complexity of O(n) for uniformly distributed data; degrades to O(n²) in the worst case when all elements fall into the same bucket.",

      spaceNotes =
        "Uses O(n + k) additional memory, where k is the number of buckets; requires auxiliary list structures and additional buffer space.",

      prosAndCons = (
        List(
          "Expected O(n) time complexity for uniformly distributed data",
          "Good practical performance for random input",
          "Straightforward implementation using bucket structures",
          "Can achieve very high throughput for large datasets"
        ),
        List(
          "Worst-case complexity of O(n²)",
          "Assumes a known or uniform distribution of data",
          "Additional memory consumption",
          "Restricted to data within a known range or requiring normalisation"
        )
      )
    )
  )