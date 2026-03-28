package algorithms

import model.SortStep
import model.SortStep.*

object CocktailSort extends SortAlgorithm:
  val name = "Cocktail Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] =
    val a = arr.clone()
    val buf = collection.mutable.ListBuffer.empty[SortStep]
    var swapped = true
    var start = 0
    var end = a.length - 1
    while swapped do
      swapped = false
      for i <- start until end do
        buf += Compare(i, i + 1)
        if a(i) > a(i + 1) then
          val tmp = a(i); a(i) = a(i + 1); a(i + 1) = tmp
          buf += Swap(i, i + 1); swapped = true
      buf += MarkSorted(end); end -= 1
      if swapped then
        swapped = false
        for i <- end - 1 to start by -1 do
          buf += Compare(i, i + 1)
          if a(i) > a(i + 1) then
            val tmp = a(i); a(i) = a(i + 1); a(i + 1) = tmp
            buf += Swap(i, i + 1); swapped = true
        buf += MarkSorted(start); start += 1
    buf += Done
    buf.to(LazyList)