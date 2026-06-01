package algorithms

import model.SortStep

trait SortAlgorithm:
  def name: String
  def steps(arr: Array[Int]): LazyList[SortStep]