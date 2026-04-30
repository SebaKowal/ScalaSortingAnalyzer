package benchmark

import algorithms.pure.{PureAlgorithmRegistry, PureSorting}
import model.{AlgorithmType, ArrayGenerator, GeneratorType}

/**
 * Validates pure algorithm implementations against reference sort.
 * Run before any benchmarking — never benchmark incorrect code.
 *
 * Timeout per run prevents infinite loops from hanging the validator.
 */
object CorrectnessValidator:

  private val TimeoutMs = 10_000L

  case class ValidationResult(
                               algoName:  String,
                               pattern:   String,
                               size:      Int,
                               passed:    Boolean,
                               message:   String,
                               inputSnap: Array[Int] = Array.empty
                             )

  def validateAll(): Seq[ValidationResult] =
    val results = collection.mutable.ArrayBuffer.empty[ValidationResult]

    val edgeCases: Seq[(String, Array[Int])] = Seq(
      ("empty",          Array.empty[Int]),
      ("single",         Array(42)),
      ("two_sorted",     Array(1, 2)),
      ("two_reversed",   Array(2, 1)),
      ("all_equal",      Array(7, 7, 7, 7, 7)),
      ("two_equal",      Array(3, 3)),
      ("already_sorted", Array(1, 2, 3, 4, 5, 6, 7, 8)),
      ("reverse_sorted", Array(8, 7, 6, 5, 4, 3, 2, 1)),
      ("duplicates",     Array(3, 1, 4, 1, 5, 9, 2, 6, 5, 3)),
      ("large_equal",    Array.fill(100)(42)),
      ("two_alternating",Array.tabulate(100)(i => if i % 2 == 0 then 1 else 2))
    )

    AlgorithmType.values.foreach { algo =>
      edgeCases.foreach { (label, arr) =>
        results += validateWithTimeout(algo, label, arr)
      }
      // Standard sizes with all generators
      Seq(10, 100, 1000).foreach { size =>
        GeneratorType.values.foreach { gen =>
          val arr = ArrayGenerator.generate(gen, size)
          results += validateWithTimeout(algo, s"${gen.label}/n=$size", arr)
        }
      }
    }
    results.toSeq

  private def validateWithTimeout(
                                   algo:  AlgorithmType,
                                   label: String,
                                   input: Array[Int]
                                 ): ValidationResult =
    import java.util.concurrent.{FutureTask, TimeUnit}
    val task = new FutureTask[ValidationResult](() => validatePure(algo, label, input))
    val t    = new Thread(task, s"validator-${algo.label}")
    t.setDaemon(true)
    t.start()
    try
      task.get(TimeoutMs, TimeUnit.MILLISECONDS)
    catch
      case _: java.util.concurrent.TimeoutException =>
        t.interrupt()
        ValidationResult(algo.label, label, input.length,
          passed  = false,
          message = s"Timed out after ${TimeoutMs}ms — possible infinite loop")
      case ex: Exception =>
        ValidationResult(algo.label, label, input.length,
          passed  = false,
          message = s"Exception: ${Option(ex.getCause).getOrElse(ex).getMessage}")

  private def validatePure(
                            algo:  AlgorithmType,
                            label: String,
                            input: Array[Int]
                          ): ValidationResult =
    val sortFn  = PureAlgorithmRegistry.get(algo)
    val working = input.clone()

    try
      sortFn(working)  // Sort in place

      val reference = input.clone()
      java.util.Arrays.sort(reference)

      if java.util.Arrays.equals(working, reference) then
        ValidationResult(algo.label, label, input.length, passed = true, "OK")
      else
        val inp = input.take(20).mkString("[", ", ", if input.length > 20 then "…]" else "]")
        val got = working.take(20).mkString("[", ", ", if working.length > 20 then "…]" else "]")
        val exp = reference.take(20).mkString("[", ", ", if reference.length > 20 then "…]" else "]")
        ValidationResult(algo.label, label, input.length,
          passed     = false,
          message    = s"Wrong output.\n  Input:    $inp\n  Got:      $got\n  Expected: $exp",
          inputSnap  = input.take(20))
    catch
      case ex: Exception =>
        val snap = input.take(20).mkString("[", ", ", "]")
        ValidationResult(algo.label, label, input.length,
          passed    = false,
          message   = s"${ex.getClass.getSimpleName}: ${ex.getMessage} | Input: $snap",
          inputSnap = input.take(20))