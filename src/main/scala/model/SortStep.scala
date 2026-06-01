package model

enum SortStep:
  case Compare(i: Int, j: Int)
  case Swap(i: Int, j: Int)
  case Set(index: Int, value: Int)
  case MarkSorted(index: Int)
  case MarkSortedRange(from: Int, to: Int)
  case ClearHighlights
  case Done
  case CountIncrement(value: Int)
  case CountSet(index: Int, value: Int)
  case BucketInsert(bucket: Int, value: Int)
  case BucketSet(bucket: Int, values: List[Int])

