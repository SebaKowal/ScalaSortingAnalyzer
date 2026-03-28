package model

/** A single animation frame produced by a sorting algorithm. */
enum SortStep:
  /** Indices currently being compared */
  case Compare(i: Int, j: Int)
  /** A swap between two indices */
  case Swap(i: Int, j: Int)
  /** Overwrite a single index with a value (used by merge / shell) */
  case Set(index: Int, value: Int)
  /** Mark one index as fully sorted */
  case MarkSorted(index: Int)
  /** Mark a range as fully sorted */
  case MarkSortedRange(from: Int, to: Int)
  /** Reset all highlights */
  case ClearHighlights
  /** Algorithm finished */
  case Done