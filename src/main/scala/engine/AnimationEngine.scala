package engine

import app.AppState
import algorithms.AlgorithmRegistry
import model.SortStep
import model.SortStep.*
import scalafx.animation.AnimationTimer

class AnimationEngine(
                       state: AppState,
                       onArrayChanged: Array[Int] => Unit,
                       onHighlight: (Option[Int], Option[Int]) => Unit,
                       onSorted: Int => Unit,
                       onSet: (Int, Int) => Unit,
                       onDone: () => Unit
                     ):
  private var steps: Iterator[SortStep] = Iterator.empty
  private var lastNanos: Long  = 0L
  private var startNanos: Long = 0L

  private var timer: AnimationTimer = null.asInstanceOf[AnimationTimer]
  timer = AnimationTimer { now =>
    if !state.isPaused.value then
      val delayNanos = state.animationSpeed.value * 1_000_000L
      if now - lastNanos >= delayNanos then
        lastNanos = now
        if steps.hasNext then
          processStep(steps.next())
          state.elapsedMs.value = (now - startNanos) / 1_000_000L
        else
          timer.stop()
  }

  def start(arr: Array[Int]): Unit =
    val algo = AlgorithmRegistry.get(state.selectedAlgorithm.value)
    steps      = algo.steps(arr).iterator
    state.comparisons.value = 0
    state.swaps.value       = 0
    state.elapsedMs.value   = 0
    state.isRunning.value   = true
    state.isPaused.value    = false
    startNanos = System.nanoTime()
    lastNanos  = startNanos
    timer.start()

  def pause(): Unit  = state.isPaused.value = true
  def resume(): Unit = state.isPaused.value = false

  def stop(): Unit =
    timer.stop()
    state.isRunning.value = false
    state.isPaused.value  = false

  protected def processStep(step: SortStep): Unit = step match
    case Compare(i, j) =>
      state.comparisons.value += 1
      onHighlight(Some(i), Some(j))
    case Swap(i, j) =>
      state.swaps.value += 1
      onHighlight(Some(i), Some(j))
      onArrayChanged(Array(i, j))
    case Set(idx, value) =>
      onSet(idx, value)
      onHighlight(Some(idx), None)
    case MarkSorted(idx) =>
      onSorted(idx)
      onHighlight(None, None)
    case MarkSortedRange(from, to) =>
      for i <- from to to do onSorted(i)
      onHighlight(None, None)
    case ClearHighlights =>
      onHighlight(None, None)
    case Done =>
      onDone()
      state.isRunning.value = false