package model

import scala.util.Random

object ArrayGenerator:

  def generate(genType: GeneratorType, size: Int): Array[Int] =
    genType match
      case GeneratorType.Random        => random(size)
      case GeneratorType.Sorted        => sorted(size)
      case GeneratorType.SortedReverse => sortedReverse(size)
      case GeneratorType.FewUnique     => fewUnique(size)
      case GeneratorType.NearlySorted  => nearlySorted(size)
      case GeneratorType.Pyramid       => pyramid(size)
      case GeneratorType.TwoHalves     => twoHalves(size)

  private def random(n: Int): Array[Int] =
    Array.fill(n)(Random.nextInt(500) + 10)

  private def sorted(n: Int): Array[Int] =
    val step = 500.0 / n
    Array.tabulate(n)(i => (10 + i * step).toInt)

  private def sortedReverse(n: Int): Array[Int] =
    sorted(n).reverse

  private def fewUnique(n: Int): Array[Int] =
    // Only 4-6 distinct values spread across the whole array
    val buckets = 5
    val values  = Array.tabulate(buckets)(i => 50 + i * 100)
    Array.fill(n)(values(Random.nextInt(buckets)))

  private def nearlySorted(n: Int): Array[Int] =
    val a     = sorted(n)
    val swaps = (n * 0.05).toInt.max(1)   // swap ~5% of elements
    for _ <- 0 until swaps do
      val i = Random.nextInt(n)
      val j = Random.nextInt(n)
      val tmp = a(i); a(i) = a(j); a(j) = tmp
    a

  private def pyramid(n: Int): Array[Int] =
    // Values rise to a peak in the middle then fall back down
    val half = n / 2
    Array.tabulate(n) { i =>
      val dist = half - math.abs(i - half)
      (10 + dist * (490.0 / half.max(1))).toInt
    }

  private def twoHalves(n: Int): Array[Int] =
    // Left half sorted low→high, right half sorted low→high independently
    // Creates a merge-sort friendly worst-case for other algorithms
    val half  = n / 2
    val left  = sorted(half)
    val right = sorted(n - half)
    left ++ right