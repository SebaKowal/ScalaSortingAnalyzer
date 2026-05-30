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
    AlgorithmType.InsertionSort -> AlgorithmInfo(
      AlgorithmType.InsertionSort,
      "Builds the sorted array one element at a time by inserting each into its correct position.",
      "Excellent for small or nearly sorted arrays. Used internally by Timsort."
    ),
    AlgorithmType.HeapSort -> AlgorithmInfo(
      AlgorithmType.HeapSort,
      "Builds a max-heap, then extracts the maximum repeatedly to produce sorted output.",
      "O(n log n) guaranteed. Not stable, but in-place."
    ),
    AlgorithmType.QuickSort -> AlgorithmInfo(
      AlgorithmType.QuickSort,
      "Picks a pivot, partitions the array around it, then recursively sorts each partition.",
      "Fastest in practice for average cases. Watch out for sorted input without pivot randomization."
    ),
    AlgorithmType.CountingSort -> AlgorithmInfo(
      AlgorithmType.CountingSort,
      "Counts occurrences of each value, then reconstructs the sorted array from the frequency table.",
      "Blazing fast when the value range k is small. Not comparison-based, so it beats O(n log n) limits."
    ),
    AlgorithmType.BucketSort -> AlgorithmInfo(
      AlgorithmType.BucketSort,
      "Distributes elements into uniformly sized buckets, sorts each bucket individually, and concatenates the results.",
      "Linear time O(n) on average for uniformly distributed data. Performance degrades to O(n²) in the worst case."
    )
  )