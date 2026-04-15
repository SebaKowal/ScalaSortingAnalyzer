package algorithms

import model.SortStep

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

object StepChannel:

  private val QueueCapacity = 512
  private val activeProducer = new AtomicReference[Thread](null)

  def cancelActiveProducer(): Unit =
    Option(activeProducer.getAndSet(null)).foreach(_.interrupt())

  def produce(fn: (SortStep => Unit) => Unit): LazyList[SortStep] =
    val queue = new LinkedBlockingQueue[Option[SortStep]](QueueCapacity)

    val producer = new Thread(
      () =>
        try fn(step => queue.put(Some(step)))
        catch case _: InterruptedException => Thread.currentThread().interrupt()
        finally
          if Thread.currentThread().isInterrupted then queue.offer(None)
          else queue.put(None),
      "sort-step-producer"
    )
    producer.setDaemon(true)
    activeProducer.set(producer)
    producer.start()

    def drain: LazyList[SortStep] =
      try queue.take() match
        case None       => LazyList.empty
        case Some(step) => step #:: drain
      catch
        case _: InterruptedException => LazyList.empty

    drain
