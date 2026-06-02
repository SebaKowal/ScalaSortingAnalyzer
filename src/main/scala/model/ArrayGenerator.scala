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
    Array.fill(n)(Random.nextInt(n.max(1)) + 1)

  private def sorted(n: Int): Array[Int] =
    Array.tabulate(n)(i => i + 1)

  private def sortedReverse(n: Int): Array[Int] =
    sorted(n).reverse

  private def fewUnique(n: Int): Array[Int] =
    val buckets = 5
    val step    = (n / 10).max(1)
    val values  = Array.tabulate(buckets)(i => 1 + i * step)
    Array.fill(n)(values(Random.nextInt(buckets)))

  private def nearlySorted(n: Int): Array[Int] =
    val a     = sorted(n)
    val swaps = (n * 0.05).toInt.max(1)
    for _ <- 0 until swaps do
      val i = Random.nextInt(n)
      val j = Random.nextInt(n)
      val tmp = a(i); a(i) = a(j); a(j) = tmp
    a


  private def pyramid(n: Int): Array[Int] =
    val half = n / 2
    val step = if half > 0 then (n - 1).toDouble / half else 1.0
    Array.tabulate(n) { i =>
      val dist = half - math.abs(i - half)
      (1 + dist * step).toInt
    }

  private def twoHalves(n: Int): Array[Int] =
    val half  = n / 2
    val left  = sorted(half)
    val right = sorted(n - half)
    left ++ right