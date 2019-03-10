package tga.folder_sync.sync

import java.security.InvalidParameterException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import java.util.function.BiFunction
import java.util.function.Supplier
import kotlin.concurrent.withLock

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */

fun <T> completableFutureViaSupplyAsync(executor: Executor, supplier: () -> T): CompletableFuture<T> =
    CompletableFuture.supplyAsync<T>(Supplier<T>{ supplier.invoke() }, executor)

fun <T, U> CompletableFuture<T>.handleAsync(executor: Executor, biFunction: (T?, Throwable?) -> U): CompletableFuture<U> =
    this.handleAsync(BiFunction<T?, Throwable?, U>{ t, u -> biFunction.invoke(t, u) }, executor)


abstract class Actor<T> {

    val stopped = AtomicBoolean(false)
    fun stop() { stopped.set(true) }

    private var wasPerformedOnce = AtomicBoolean(false)

    fun performAsync(): CompletableFuture<T> {
        if (!wasPerformedOnce.compareAndSet(false, true)) throw MasterActorCanBePerformedOnlyOnce()

        return CompletableFuture.supplyAsync{ perform() }
    }

    abstract fun perform(): T

}


class CounterLatch(initValue: Int) {

    private val counter = AtomicInteger(initValue)
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    init {
        if (initValue < 0) throw InvalidParameterException("CounterLatch initial value can't be negative.")
    }

    fun increment(delta: Int = 1) {
        counter.addAndGet(delta)
    }

    fun countDown() {
        if (counter.getAndDecrement() == 1) { // current value == 0
            lock.withLock {
               condition.signalAll()
            }
        }
    }

    fun reset() {
        counter.set(0)
        lock.withLock { condition.signalAll() }
    }

    fun await() {
        lock.withLock {
            while (counter.get() > 0) {
                condition.await(10, TimeUnit.SECONDS)
            }
        }
    }

}