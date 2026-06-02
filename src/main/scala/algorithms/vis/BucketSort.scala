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
      emit(Done)
    else
      val minVal = a.min
      val maxVal = a.max
      val range  = (maxVal - minVal).toDouble

      val bucketCount = math.max(1, n / 2)
      val buckets = Array.fill(bucketCount)(collection.mutable.ArrayBuffer.empty[Int])

      for v <- a do
        val idx =
          if range == 0 then 0
          else ((v - minVal) / range * (bucketCount - 1)).toInt

        buckets(idx) += v
        emit(BucketInsert(idx, v))

      var pos = 0
      val bucketRanges = collection.mutable.ArrayBuffer[(Int, Int)]()

      for bucket <- buckets do
        val start = pos
        for v <- bucket do
          a(pos) = v
          emit(Set(pos, v))
          pos += 1
        if start < pos then bucketRanges += ((start, pos - 1))

      for (start, end) <- bucketRanges do
        for i <- start + 1 to end do
          var j = i
          var keep = true
          while j > start && keep do
            emit(Compare(j - 1, j))
            if a(j - 1) > a(j) then
              val tmp = a(j)
              a(j) = a(j - 1)
              a(j - 1) = tmp
              emit(Swap(j - 1, j))
              j -= 1
            else
              keep = false

        for i <- start to end do emit(MarkSorted(i))

      emit(Done)
  }
