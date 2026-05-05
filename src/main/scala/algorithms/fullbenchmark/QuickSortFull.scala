package algorithms.fullbenchmark

import model.SortStep
import scala.collection.mutable.ArrayBuffer

/**
 * Wersja FULL QuickSorta.
 * Nie jest używana do mierzenia czasu (od tego masz wersję PURE),
 * ale do generowania kompletnego śladu (trace) wykonania algorytmu.
 */
object QuickSortFull:

  def steps(arr: Array[Int]): Seq[SortStep] =
    val history = ArrayBuffer.empty[SortStep]
    val workingCopy = arr.clone()

    def quickSort(low: Int, high: Int): Unit =
      if low < high then
        // Wybieramy pivot (tutaj ostatni element)
        val pIdx = partition(low, high)
        quickSort(low, pIdx - 1)
        quickSort(pIdx + 1, high)

    def partition(low: Int, high: Int): Int =
      val pivot = workingCopy(high)
      // Zapisujemy dostęp do pivota jako operację pomocniczą (opcjonalnie)
      var i = low - 1

      for j <- low until high do
        // 1. REJESTRUJEMY PORÓWNANIE[cite: 15]
        history += SortStep.Compare(j, high)
        if workingCopy(j) <= pivot then
          i += 1
          swap(i, j)

      swap(i + 1, high)
      i + 1

    def swap(idx1: Int, idx2: Int): Unit =
      if idx1 != idx2 then
        // 2. REJESTRUJEMY ZAMIANĘ[cite: 15]
        history += SortStep.Swap(idx1, idx2)
        val temp = workingCopy(idx1)
        workingCopy(idx1) = workingCopy(idx2)
        workingCopy(idx2) = temp
      else
        // Nawet jeśli indeksy są te same, niektóre analizy chcą wiedzieć o "dotknięciu"
        history += SortStep.Set(idx1, workingCopy(idx1))

    quickSort(0, workingCopy.length - 1)
    history.toSeq