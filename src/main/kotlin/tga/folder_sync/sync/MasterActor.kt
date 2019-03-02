package tga.folder_sync.sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */

class MasterActor(
    val commandsSource: Sequence<SyncCmd>
) {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    val PARALLEL_COPY_FILES = 2

    val cmdExecutor = Executors.newFixedThreadPool(PARALLEL_COPY_FILES)
    val observersExecutor = Executors.newWorkStealingPool (PARALLEL_COPY_FILES * 2)


    private val stopped = AtomicBoolean(false)
            fun stop() { stopped.set(true) }

    private var wasPerformedOnce = AtomicBoolean(false)

    fun performAsync(): CompletableFuture<Void> {

        synchronized(this) {
            if ( wasPerformedOnce.get() ) throw MasterActorCanBePerformedOnlyOnce()
            wasPerformedOnce.set(true)
        }

        return CompletableFuture.runAsync {
            handlingCommands()
            shutdownThreads()
        }

    }

    private fun handlingCommands() {
        val cmds = commandsSource.iterator()

        while ( cmds.hasNext() && !stopped.get()) {
            val currentCmd = cmds.next()
            completableFutureViaSupplyAsync(cmdExecutor){             exec(currentCmd) } // exec command
                         .handleAsync(observersExecutor){ cmd, err -> report(cmd, err)  } // report about the execution
        }
    }

    private fun exec(cmd: SyncCmd) {
        logger.trace("exec({})",cmd)
        Thread.sleep(1000)

/*
        val cmdTask = completableFutureViaSupplyAsync(cmdExecutor){
            var err: Throwable? = null

            try {
                cmd.performCommand()
            } catch (e: Throwable) {
                err = e
            }

            if (err == null) {
                //todo: write success log
            } else {
                //todo: write error log
            }

        }
*/


    }

    private fun report(cmd: Unit, err: Throwable?) {
        logger.trace("report({}, {})", cmd, err)
    }

    private fun shutdownThreads() {
        logger.info("Waiting for current tasks finishing...")

        cmdExecutor.shutdown()
        observersExecutor.shutdown()

        val doneCmdExecutor = cmdExecutor.awaitTermination(15, TimeUnit.SECONDS)
        val doneObserversExecutor = observersExecutor.awaitTermination(5, TimeUnit.SECONDS)

        if (!doneCmdExecutor || !doneObserversExecutor) {
            if (!doneCmdExecutor      ) logger.warn("Current sync tasks were not shutdown correctly!")
            if (!doneObserversExecutor) logger.warn("Current observer tasks were not shutdown correctly!")
        } else {
            logger.info("Current tasks were finished successfully")
        }
    }

}



