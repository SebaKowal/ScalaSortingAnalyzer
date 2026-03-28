package app

import scalafx.beans.property.*
import model.AlgorithmType
import model.GeneratorType

class AppState:
  val arraySize: IntegerProperty      = IntegerProperty(50)
  val animationSpeed: IntegerProperty = IntegerProperty(50)
  val selectedAlgorithm: ObjectProperty[AlgorithmType] =
    ObjectProperty(AlgorithmType.BubbleSort)
  val selectedGenerator: ObjectProperty[GeneratorType] =
    ObjectProperty(GeneratorType.Random)
  val isRunning: BooleanProperty   = BooleanProperty(false)
  val isPaused: BooleanProperty    = BooleanProperty(false)
  val comparisons: LongProperty    = LongProperty(0L)
  val swaps: LongProperty          = LongProperty(0L)
  val elapsedMs: LongProperty      = LongProperty(0L)
  val statusMessage: StringProperty = StringProperty("Ready")

object AppState:
  val instance: AppState = AppState()