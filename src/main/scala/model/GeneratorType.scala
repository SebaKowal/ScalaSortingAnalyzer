package model

enum GeneratorType(val label: String):
  case Random         extends GeneratorType("Random")
  case Sorted         extends GeneratorType("Sorted (Low→High)")
  case SortedReverse  extends GeneratorType("Sorted (High→Low)")
  case FewUnique      extends GeneratorType("Few Unique Values")
  case NearlySorted   extends GeneratorType("Nearly Sorted")
  case Pyramid        extends GeneratorType("Pyramid")
  case TwoHalves      extends GeneratorType("Two Sorted Halves")