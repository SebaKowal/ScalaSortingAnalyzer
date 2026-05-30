package model

enum AlgorithmType(val label: String, val worstCase: String, val bestCase: String, val avgCase: String, val space: String):
  case BubbleSort    extends AlgorithmType("Bubble Sort",    "O(n²)",     "O(n)",       "O(n²)",      "O(1)")
  case BucketSort    extends AlgorithmType("Bucket Sort",    "O(n²)",     "O(n + k)",   "O(n)",       "O(n + k)")
  case CountingSort  extends AlgorithmType("Counting Sort",  "O(n + k)",  "O(n + k)",   "O(n + k)",   "O(k)")
  case HeapSort      extends AlgorithmType("Heap Sort",      "O(n log n)", "O(n log n)", "O(n log n)", "O(1)")
  case InsertionSort extends AlgorithmType("Insertion Sort", "O(n²)",     "O(n)",       "O(n²)",      "O(1)")
  case QuickSort     extends AlgorithmType("Quick Sort",     "O(n²)",     "O(n log n)", "O(n log n)", "O(log n)")