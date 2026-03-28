package algorithms

import model.SortStep

/** All sorting algorithms implement this trait.
 *  They return a lazy sequence of SortStep so the animator
 *  can play them at any speed without re-running the sort. */
trait SortAlgorithm:
  def name: String
  /** Produce all steps for sorting a *copy* of the given array. */
  def steps(arr: Array[Int]): LazyList[SortStep]