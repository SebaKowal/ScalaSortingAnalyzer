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
        "Bubble Sort ma pesymistyczną i średnią złożoność O(n²). W najlepszym przypadku (gdy dane są już posortowane i zastosujemy wczesne zakończenie) może działać w O(n).",

      spaceNotes =
        "Algorytm działa w miejscu i wykorzystuje O(1) dodatkowej pamięci.",

      prosAndCons = (
        List(
          "Bardzo prosta implementacja",
          "Dobre lokalne wykorzystanie pamięci – operuje na sąsiadach",
          "Łatwy do analizy i wizualizacji"
        ),
        List(
          "Pesymistyczna i średnia złożoność O(n²)",
          "Możliwość wykonywania ogromnej liczby zbędnych porównań i zamian",
          "Bardzo słaba praktyczna wydajność – rzadko stosowany w realnych systemach"
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
        "Średnia złożoność O(n log n). W najgorszym przypadku (zły pivot) O(n²), dlatego stosuje się randomizację lub median-of-three.",

      spaceNotes =
        "Sortowanie w miejscu (in-place). Wymaga O(log n) stosu rekurencji w przypadku średnim, O(n) w najgorszym.",

      prosAndCons = (
        List(
          "Bardzo szybki w praktyce (średnio O(n log n))",
          "Dobre wykorzystanie pamięci podręcznej (cache-friendly)",
          "Małe stałe w złożoności asymptotycznej",
          "Szeroko stosowany w bibliotekach standardowych"
        ),
        List(
          "Pesymistycznie O(n²)",
          "Niestabilny",
          "Zależny od wyboru pivota",
          "Możliwość przepełnienia stosu przy głębokiej rekursji"
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
        "Insertion Sort w najlepszym przypadku osiąga O(n), gdy dane są już częściowo lub całkowicie posortowane. W przypadku średnim i najgorszym działa w O(n²).",

      spaceNotes =
        "Algorytm działa w miejscu i wymaga jedynie O(1) dodatkowej pamięci.",

      prosAndCons = (
        List(
          "Bardzo szybki dla prawie posortowanych danych – O(n)",
          "Stabilny algorytm",
          "Prosta implementacja",
          "Działa w miejscu (O(1) pamięci)"
        ),
        List(
          "Najgorszy i średni czas O(n²)",
          "Wymaga wielu przesunięć elementów",
          "Słabo skaluje się dla dużych zbiorów danych",
          "Wrażliwy na dane odwrotnie posortowane"
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
        "Budowa kopca ma złożoność O(n). Każde usunięcie elementu (ekstrakcja) wymaga O(log n), co daje łączną złożoność O(n log n) we wszystkich przypadkach (najlepszym, średnim i najgorszym).",

      spaceNotes =
        "Sortowanie w miejscu (in-place) z użyciem O(1) dodatkowej pamięci. Stos wywołań rekurencyjnych dla procedury heapify ma złożoność O(log n).",

      prosAndCons = (
        List(
          "Gwarantowane O(n log n) w każdym przypadku",
          "Sortowanie w miejscu (O(1) dodatkowej pamięci)",
          "Brak degeneracji do O(n²)",
          "Dobre do struktur typu priority queue"
        ),
        List(
          "Brak stabilności",
          "Słaba lokalność pamięci (cache misses)",
          "W praktyce wolniejszy niż QuickSort",
          "Bardziej złożona implementacja"
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
        "liniowa złożoność czasowa; brak operacji porównywania elementów; wysoka wydajność dla małych zakresów danych",

      spaceNotes =
        "konieczność znajomości zakresu danych; dodatkowe zużycie pamięci; ograniczenie do danych liczbowych o niewielkim zakresie wartości",

      prosAndCons = (
        List(
          "liniowa złożoność czasowa",
          "stabilność algorytmu",
          "brak operacji porównywania elementów",
          "wysoka wydajność dla małych zakresów danych"
        ),
        List(
          "konieczność znajomości zakresu danych",
          "dodatkowe zużycie pamięci",
          "ograniczenie do danych liczbowych o niewielkim zakresie wartości",
          "mniejsza uniwersalność niż algorytmy porównawcze"
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
        "oczekiwana złożoność O(n) przy jednostajnym rozkładzie danych; w pesymistycznym przypadku O(n²) gdy wszystkie elementy trafiają do jednego kubełka",

      spaceNotes =
        "dodatkowe zużycie pamięci O(n + k), gdzie k to liczba kubełków; wymaga struktury list/kubełków oraz pamięci pomocniczej",

      prosAndCons = (
        List(
          "oczekiwana złożoność O(n) dla jednostajnego rozkładu danych",
          "dobra wydajność w praktyce dla danych losowych",
          "prosta implementacja przy użyciu kubełków",
          "możliwość bardzo wysokiej wydajności dla dużych zbiorów"
        ),
        List(
          "złożoność O(n²) w pesymistycznym przypadku",
          "wymaga założenia o rozkładzie danych",
          "dodatkowe zużycie pamięci",
          "ograniczenie do danych z określonego przedziału lub normalizacji"
        )
      )
    )
  )