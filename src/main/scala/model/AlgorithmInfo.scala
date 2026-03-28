package model

case class AlgorithmInfo(
                          algorithmType: AlgorithmType,
                          description: String,
                          tip: String
                        )

object AlgorithmInfo:
  val all: Map[AlgorithmType, AlgorithmInfo] = Map(
    AlgorithmType.BubbleSort -> AlgorithmInfo(
      AlgorithmType.BubbleSort,
      "Repeatedly steps through the list, compares adjacent elements and swaps them if they are in the wrong order.",
      "Simple but inefficient. Best for nearly sorted data or educational use."
    ),
    AlgorithmType.SelectionSort -> AlgorithmInfo(
      AlgorithmType.SelectionSort,
      "Divides the array into sorted and unsorted parts; repeatedly selects the minimum from unsorted.",
      "Always O(n²) comparisons regardless of input. Minimizes swaps."
    ),
    AlgorithmType.InsertionSort -> AlgorithmInfo(
      AlgorithmType.InsertionSort,
      "Builds the sorted array one element at a time by inserting each into its correct position.",
      "Excellent for small or nearly sorted arrays. Used internally by Timsort."
    ),
    AlgorithmType.MergeSort -> AlgorithmInfo(
      AlgorithmType.MergeSort,
      "Divides array in half, recursively sorts both halves, then merges them.",
      "Stable and consistent O(n log n). Requires O(n) extra space."
    ),
    AlgorithmType.QuickSort -> AlgorithmInfo(
      AlgorithmType.QuickSort,
      "Picks a pivot, partitions the array around it, then recursively sorts each partition.",
      "Fastest in practice for average cases. Watch out for sorted input without pivot randomization."
    ),
    AlgorithmType.HeapSort -> AlgorithmInfo(
      AlgorithmType.HeapSort,
      "Builds a max-heap, then extracts the maximum repeatedly to produce sorted output.",
      "O(n log n) guaranteed. Not stable, but in-place."
    ),
    AlgorithmType.ShellSort -> AlgorithmInfo(
      AlgorithmType.ShellSort,
      "Generalizes insertion sort by sorting elements far apart first, then reducing the gap.",
      "Much faster than insertion sort. Gap sequence affects performance significantly."
    ),
    AlgorithmType.CocktailSort -> AlgorithmInfo(
      AlgorithmType.CocktailSort,
      "Bidirectional bubble sort — passes alternately left-to-right and right-to-left.",
      "Handles 'turtles' (small values at the end) better than classic bubble sort."
    )
  )