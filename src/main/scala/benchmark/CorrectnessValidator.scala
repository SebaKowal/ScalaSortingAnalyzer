package benchmark

import model.{AlgorithmType, ArrayGenerator, GeneratorType, SortStep}
import algorithms.AlgorithmRegistry
import java.util.concurrent.{Callable, FutureTask, TimeUnit}
import java.util.concurrent.TimeoutException

/** Validates algorithm correctness before any benchmarking.
 *  Returns None if correct, Some(errorMessage) if not. */
object CorrectnessValidator:

  case class ValidationResult(
                               algoName:  String,
                               pattern:   String,
                               size:      Int,
                               passed:    Boolean,
                               message:   String,
                               inputSnap: Array[Int] = Array.empty  // first 20 elements for diagnosis
                             )

  /** Run full correctness suite — all algos, multiple patterns, edge cases */
  def validateAll(): Seq[ValidationResult] =
    val results = collection.mutable.ArrayBuffer.empty[ValidationResult]

    // Edge cases always checked
    val edgeCases: Seq[(String, Array[Int])] = Seq(
      ("empty",          Array.empty[Int]),
      ("single",         Array(42)),
      ("two_sorted",     Array(1, 2)),
      ("two_reversed",   Array(2, 1)),
      ("all_equal",      Array(5, 5, 5, 5, 5)),
      ("two_equal",      Array(3, 3)),
      ("already_sorted", Array(1, 2, 3, 4, 5, 6, 7, 8)),
      ("reverse_sorted", Array(8, 7, 6, 5, 4, 3, 2, 1)),
      ("duplicates",     Array(3, 1, 4, 1, 5, 9, 2, 6, 5, 3))
    )

    AlgorithmType.values.foreach { algo =>
      // Edge cases
      edgeCases.foreach { (name, arr) =>
        results += validate(algo, name, arr)
      }
      // Standard sizes
      Seq(10, 100, 500).foreach { size =>
        GeneratorType.values.foreach { gen =>
          val arr = ArrayGenerator.generate(gen, size)
          results += validate(algo, s"${gen.label}/n=$size", arr)
        }
      }
    }
    results.toSeq

  private val ValidationTimeoutSec = 5L

  def validate(
                algo:    AlgorithmType,
                label:   String,
                input:   Array[Int]
              ): ValidationResult =
    val task = new FutureTask[ValidationResult](() => validateUnsafe(algo, label, input))
    val thread = new Thread(task, "validator-worker")
    thread.setDaemon(true)
    thread.start()
    try
      task.get(ValidationTimeoutSec, TimeUnit.SECONDS)
    catch
      case _: TimeoutException =>
        thread.interrupt()
        ValidationResult(algo.label, label, input.length,
          passed = false, s"Timed out after ${ValidationTimeoutSec}s — possible infinite loop")
      case ex: Exception =>
        ValidationResult(algo.label, label, input.length,
          passed = false, s"Unexpected error: ${ex.getCause match
            case null => ex.getMessage
            case c    => c.getMessage
          }")

  private def validateUnsafe(
                algo:    AlgorithmType,
                label:   String,
                input:   Array[Int]
              ): ValidationResult =
    val working = input.clone()

    try
      AlgorithmRegistry.get(algo).steps(input).foreach {
        case SortStep.Swap(i, j)      =>
          val tmp = working(i); working(i) = working(j); working(j) = tmp
        case SortStep.Set(idx, value) =>
          working(idx) = value
        case _ =>
      }

      val reference = input.clone()
      java.util.Arrays.sort(reference)

      val matchesReference = java.util.Arrays.equals(working, reference)
      val isSortedAsc      = checkSortedAsc(working)

      if matchesReference && isSortedAsc then
        ValidationResult(algo.label, label, input.length, passed = true, "OK")
      else
        val snap    = input.take(20).mkString("[", ", ", if input.length > 20 then "…]" else "]")
        val gotSnap = working.take(20).mkString("[", ", ", if working.length > 20 then "…]" else "]")
        val msg =
          if !matchesReference then
            s"Output does not match reference sort. Input: $snap | Got: $gotSnap"
          else
            s"Output is not ascending. Got: $gotSnap"
        ValidationResult(algo.label, label, input.length,
          passed = false, msg, input.take(20))

    catch
      case ex: Exception =>
        val snap = input.take(20).mkString("[", ", ", "]")
        ValidationResult(algo.label, label, input.length,
          passed = false, s"Exception: ${ex.getMessage} | Input: $snap", input.take(20))

  private def checkSortedAsc(arr: Array[Int]): Boolean =
    if arr.length <= 1 then return true
    var i = 1
    while i < arr.length do
      if arr(i) < arr(i - 1) then return false
      i += 1
    true