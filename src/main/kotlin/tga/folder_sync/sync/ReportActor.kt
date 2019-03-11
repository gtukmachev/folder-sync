package tga.folder_sync.sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by grigory@clearscale.net on 3/2/2019.
 */
typealias ReportItem = Pair<SyncCmd?, Throwable?>

class ReportActor : Actor<Unit>() {

    companion object {
        private val WAIT_FACTOR_TIMEOUT = 5L //seconds
        private val WAIT_FACTOR_SIZE = 1 //max items in queue
    }


    private val logger: Logger = LoggerFactory.getLogger("report")


    private val queue = ConcurrentLinkedQueue<ReportItem>()
    private var counterLatch = CounterLatch(WAIT_FACTOR_SIZE)

    override fun perform() {
        while (!stopped.get()) {
            counterLatch.await()
            writeReport()
            counterLatch.increment(WAIT_FACTOR_SIZE)
        }
    }

    fun push(item: ReportItem) {
        queue.add(item)
        counterLatch.countDown()
    }


    fun writeReport() {

        var item = queue.poll()
        while (item != null) {

            val (cmd, err) = item
            if (cmd != null) {
                logger.info("{}", cmd)
            } else if (err != null) {
                logger.error("{} ::: ${err.javaClass.simpleName} '${err.message}'", cmd)
            }

            item = queue.poll()
        }

    }


}