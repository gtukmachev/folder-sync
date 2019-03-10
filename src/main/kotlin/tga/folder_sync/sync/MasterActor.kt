package tga.folder_sync.sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */

class MasterActor(private val commandsSource: Sequence<SyncCmd>) : Actor<Unit>() {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    private val PARALLEL_COPY_FILES = 2

    private val cmdExecutor = Executors.newFixedThreadPool(PARALLEL_COPY_FILES)
    private val reportExecutor = Executors.newFixedThreadPool(PARALLEL_COPY_FILES * 2)

    private val tasksCounterLatch = CounterLatch(0)


    override fun perform() {
        handlingCommands()
        shutdownThreads()
    }

    private fun handlingCommands() {
        val cmds = commandsSource.iterator()
        while ( cmds.hasNext() && !stopped.get()) {
            val currentCmd = cmds.next()
            tasksCounterLatch.increment()
            completableFutureViaSupplyAsync(cmdExecutor){ exec(currentCmd) }  // exec command
                            .handleAsync(reportExecutor){ cmd, err -> report(cmd, err) } // report about the execution
        }
    }

    private fun shutdownThreads() {
        logger.info("Waiting for current tasks finishing...")
        tasksCounterLatch.await()

        cmdExecutor.shutdown()
        val doneCmdExecutor = cmdExecutor.awaitTermination(15, TimeUnit.SECONDS)

        reportExecutor.shutdown()
        val doneObserversExecutor = reportExecutor.awaitTermination(5, TimeUnit.SECONDS)

        if (!doneCmdExecutor || !doneObserversExecutor) {
            if (!doneCmdExecutor      ) logger.warn("Current sync tasks were not shutdown correctly!")
            if (!doneObserversExecutor) logger.warn("Current observer tasks were not shutdown correctly!")
        } else {
            logger.info("Current tasks were finished successfully")
        }
    }



    private fun exec(cmd: SyncCmd): SyncCmd {
        Thread.sleep(Random.nextLong(1000))
        return cmd.perform()
    }

    private fun report(cmd: SyncCmd?, err: Throwable?) {
        logger.trace("report({}, {})", cmd, err)
        tasksCounterLatch.countDown()
    }

}



