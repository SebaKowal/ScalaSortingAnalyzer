package algorithms.vis

import algorithms.{SortAlgorithm, StepChannel}
import model.SortStep
import model.SortStep.*

object BucketSort extends SortAlgorithm:
  val name = "Bucket Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] = StepChannel.produce { emit =>
    val a = arr.clone()
    val n = a.length
    if n <= 1 then
      for i <- a.indices do emit(MarkSorted(i))
      emit(Done)
    else
      val minVal = a.min
      val maxVal = a.max
      val range  = (maxVal - minVal).toDouble

      // ── 1. Rozdzielenie do kubełków (wizualizujemy skanowanie) ──
      val buckets = Array.fill(n)(collection.mutable.ArrayBuffer.empty[Int])

      for i <- 0 until n do
        emit(Compare(i, i)) // Podświetla element, który aktualnie przydzielamy
        val bucketIdx =
          if range == 0 then 0
          else ((a(i) - minVal) / range * (n - 1)).toInt
        buckets(bucketIdx) += a(i)

      // ── 2. Przepisanie zawartości kubełków do tablicy (tworzenie stref) ──
      var pos = 0
      val bucketBounds = collection.mutable.ArrayBuffer[(Int, Int)]() // Pamiętamy granice kubełków

      for bucket <- buckets do
        val start = pos
        for value <- bucket do
          a(pos) = value
          emit(Set(pos, value)) // Pokazuje, jak elementy układają się w kubełkowe bloki
          pos += 1
        if start < pos then
          bucketBounds += ((start, pos - 1))

      // ── 3. Sortowanie w miejscu (Insertion Sort) każdej strefy kubełkowej ──
      for (start, end) <- bucketBounds do
        for i <- start + 1 to end do
          var j = i
          var keepGoing = true
          while j > start && keepGoing do
            emit(Compare(j - 1, j)) // Wizualizacja porównania wewnątrz kubełka
            if a(j - 1) > a(j) then
              val tmp = a(j); a(j) = a(j - 1); a(j - 1) = tmp
              emit(Swap(j - 1, j)) // Wizualizacja zamiany
              j -= 1
            else
              keepGoing = false

        // Zaznaczamy dany kubełek jako ostatecznie posortowany
        for i <- start to end do emit(MarkSorted(i))

      emit(Done)
  }