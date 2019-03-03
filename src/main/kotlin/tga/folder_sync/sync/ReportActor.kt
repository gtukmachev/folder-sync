package tga.folder_sync.sync

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Created by grigory@clearscale.net on 3/2/2019.
 */
class ReportActor : Actor<Unit>() {

    companion object {
        private val WAIT_FACTOR_TIMEOUT = 5L //seconds
        private val WAIT_FACTOR_SIZE = 100 //max items in queue
    }

    private val queue = ConcurrentLinkedQueue<Pair<SyncCmd, Throwable?>>()
    private var counterLatch = CounterLatch(WAIT_FACTOR_SIZE)

    override fun perform() {
        while (!stopped.get()) {
            counterLatch.await()
            writeReport()
            counterLatch.increment(WAIT_FACTOR_SIZE)
        }
    }

    fun push(item: Pair<SyncCmd, Throwable?>) {
        queue.add(item)
        counterLatch.countDown()
    }


    fun writeReport() {

        var item = queue.poll()
        while (item != null) {


            item = queue.poll()
        }

    }


}