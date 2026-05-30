package algorithms

import algorithms.vis.{BubbleSort, CountingSort, HeapSort, InsertionSort, QuickSort, BucketSort}
import model.AlgorithmType

object AlgorithmRegistry:
  val all: Map[AlgorithmType, SortAlgorithm] = Map(
    AlgorithmType.CountingSort    -> CountingSort,
    AlgorithmType.BucketSort -> BucketSort,
    AlgorithmType.QuickSort -> QuickSort,
    AlgorithmType.HeapSort     -> HeapSort,
    AlgorithmType.BubbleSort     -> BubbleSort,
    AlgorithmType.InsertionSort      -> InsertionSort,
  )

  def get(t: AlgorithmType): SortAlgorithm =
    all.getOrElse(t, throw new IllegalStateException(
      s"No implementation registered for $t — add it to AlgorithmRegistry.all"
    ))

