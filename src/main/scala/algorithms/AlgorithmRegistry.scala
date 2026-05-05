package algorithms

import algorithms.vis.{BubbleSort, CocktailSort, HeapSort, InsertionSort, MergeSort, QuickSort, SelectionSort, ShellSort}
import ui.utils.AlgorithmType
import algorithms.fullbenchmark.QuickSortFull

object AlgorithmRegistry:
  val all: Map[AlgorithmType, SortAlgorithm] = Map(
    AlgorithmType.BubbleSort    -> BubbleSort,
    AlgorithmType.SelectionSort -> SelectionSort,
    AlgorithmType.InsertionSort -> InsertionSort,
    AlgorithmType.MergeSort     -> MergeSort,
    AlgorithmType.QuickSort     -> QuickSort,
    AlgorithmType.HeapSort      -> HeapSort,
    AlgorithmType.ShellSort     -> ShellSort,
    AlgorithmType.CocktailSort  -> CocktailSort,
  )

  def get(t: AlgorithmType): SortAlgorithm =
    all.getOrElse(t, throw new IllegalStateException(
      s"No implementation registered for $t — add it to AlgorithmRegistry.all"
    ))