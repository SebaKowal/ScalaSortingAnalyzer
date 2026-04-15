package algorithms

import model.SortStep
import model.SortStep.*

object CocktailSort extends SortAlgorithm:
  val name = "Cocktail Sort"

  def steps(arr: Array[Int]): LazyList[SortStep] = StepChannel.produce { emit =>
    val a = arr.clone()
    var swapped = true
    var start = 0
    var end = a.length - 1
    while swapped do
      swapped = false
      for i <- start until end do
        emit(Compare(i, i + 1))
        if a(i) > a(i + 1) then
          val tmp = a(i); a(i) = a(i + 1); a(i + 1) = tmp
          emit(Swap(i, i + 1)); swapped = true
      emit(MarkSorted(end)); end -= 1
      if swapped then
        swapped = false
        for i <- end - 1 to start by -1 do
          emit(Compare(i, i + 1))
          if a(i) > a(i + 1) then
            val tmp = a(i); a(i) = a(i + 1); a(i + 1) = tmp
            emit(Swap(i, i + 1)); swapped = true
        emit(MarkSorted(start)); start += 1
    emit(Done)
  }
