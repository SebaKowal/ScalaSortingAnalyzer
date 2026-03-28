package algorithms

import model.AlgorithmType

object AlgorithmRegistry:
  val all: Map[AlgorithmType, SortAlgorithm] = Map(
    AlgorithmType.BubbleSort    -> BubbleSort,
    AlgorithmType.SelectionSort -> SelectionSort,
    AlgorithmType.InsertionSort -> InsertionSort,
    AlgorithmType.MergeSort     -> MergeSort,
    AlgorithmType.QuickSort     -> QuickSort,
    AlgorithmType.HeapSort      -> HeapSort,
    AlgorithmType.ShellSort     -> ShellSort,
    AlgorithmType.CocktailSort  -> CocktailSort
  )

  def get(t: AlgorithmType): SortAlgorithm = all(t)