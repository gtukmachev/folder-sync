package tga.folder_sync.sync

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.BiFunction
import java.util.function.Supplier

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */

fun <T> completableFutureViaSupplyAsync(executor: Executor, supplier: () -> T): CompletableFuture<T> =
    CompletableFuture.supplyAsync<T>(Supplier<T>{ supplier.invoke() }, executor)

fun <T, U> CompletableFuture<T>.handleAsync(executor: Executor, biFunction: (T, Throwable?) -> U): CompletableFuture<U> =
    this.handleAsync(BiFunction<T, Throwable?, U>{ t, u -> biFunction.invoke(t, u) }, executor)
