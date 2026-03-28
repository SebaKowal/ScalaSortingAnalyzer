package model

enum AlgorithmType(val label: String, val worstCase: String, val bestCase: String, val avgCase: String, val space: String):
  case BubbleSort    extends AlgorithmType("Bubble Sort",     "O(n²)",    "O(n)",      "O(n²)",    "O(1)")
  case SelectionSort extends AlgorithmType("Selection Sort",  "O(n²)",    "O(n²)",     "O(n²)",    "O(1)")
  case InsertionSort extends AlgorithmType("Insertion Sort",  "O(n²)",    "O(n)",      "O(n²)",    "O(1)")
  case MergeSort     extends AlgorithmType("Merge Sort",      "O(n log n)","O(n log n)","O(n log n)","O(n)")
  case QuickSort     extends AlgorithmType("Quick Sort",      "O(n²)",    "O(n log n)","O(n log n)","O(log n)")
  case HeapSort      extends AlgorithmType("Heap Sort",       "O(n log n)","O(n log n)","O(n log n)","O(1)")
  case ShellSort     extends AlgorithmType("Shell Sort",      "O(n²)",    "O(n log n)","O(n log²n)","O(1)")
  case CocktailSort  extends AlgorithmType("Cocktail Sort",   "O(n²)",    "O(n)",      "O(n²)",    "O(1)")