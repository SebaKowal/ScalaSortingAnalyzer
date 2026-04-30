package algorithms.pure

import model.AlgorithmType

/**
 * Maps AlgorithmType enum to pure sorting functions.
 * Returns (Array[Int]) => Unit — sorts in place.
 * Caller is responsible for cloning input before passing.
 */
object PureAlgorithmRegistry:

  private type SortFn = Array[Int] => Unit

  val all: Map[AlgorithmType, SortFn] = Map(
    AlgorithmType.BubbleSort    -> PureSorting.bubbleSort,
    AlgorithmType.SelectionSort -> PureSorting.selectionSort,
    AlgorithmType.InsertionSort -> PureSorting.insertionSort,
    AlgorithmType.MergeSort     -> PureSorting.mergeSort,
    AlgorithmType.QuickSort     -> PureSorting.quickSort,
    AlgorithmType.HeapSort      -> PureSorting.heapSort,
    AlgorithmType.ShellSort     -> PureSorting.shellSort,
    AlgorithmType.CocktailSort  -> PureSorting.cocktailSort
  )

  def get(t: AlgorithmType): SortFn =
    all.getOrElse(t, throw new IllegalArgumentException(
      s"No pure implementation for $t — add it to PureAlgorithmRegistry"
    ))