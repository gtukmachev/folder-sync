package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.Cancellable
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.exts.readableFileSize
import tga.folder_sync.exts.sec

class StatisticCollectorActor(
    totalFiles: Int,
    totalBytes: Long
) : AbstractLoggingActor() {

    data class UpdateStatisitc(val cmd: SyncCmd, val err :Throwable?)

    private val finallyExpectedStatistic: Stat = Stat(
            files = totalFiles.takeIf{ it > 0  } ?: 1,
            bytes = totalBytes.takeIf{ it > 0L } ?: 1L
        )

    private val globalProgress  = Progress(finallyExpectedStatistic)
    private val successProgress = Progress(finallyExpectedStatistic)
    private val errorProgress   = Progress(finallyExpectedStatistic)

    lateinit var printStatisticJob: Cancellable

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(UpdateStatisitc::class.java){ updateStatAndProgress(it) }
        .match(  PrintProgress::class.java){ logProgress()       }
        .build()

    override fun preStart() {
        printStatisticJob = context.system.scheduler.scheduleAtFixedRate(
            4.sec(), 20.sec(), self(), PrintProgress(), context.dispatcher, self()
        )
    }

    override fun postStop() {
        printStatisticJob.cancel()
    }

    private fun updateStatAndProgress(result: UpdateStatisitc) {
        val fileSize = result.cmd.fileSize.toLong()

        globalProgress.add(fileSize)

        when (result.err) {
            null -> successProgress.add(fileSize)
            else -> errorProgress.add(fileSize)
        }
    }

    private fun logProgress() {
        log().info("""
                Statistic:
                     global: ${ globalProgress.toLogStr()}
                    success: ${successProgress.toLogStr()}
                     errors: ${  errorProgress.toLogStr()}
        """.trimIndent())
    }

    data class Progress(
        val expectedStat: Stat,
        val currentStat: Stat = Stat(),
        var progressFiles: Double = 0.0, var progressBytes: Double = 0.0
    ) {
        fun add(incomeBytes: Long) {
            this.currentStat.files += 1
            this.currentStat.bytes += incomeBytes
            update()
        }

        private fun update() {
            this.progressFiles = currentStat.files.toDouble() / expectedStat.files.toDouble()
            this.progressBytes = currentStat.bytes.toDouble() / expectedStat.bytes.toDouble()
        }

        fun toLogStr(): String {
            val prcFiles = "%.2f".format(this.progressFiles * 100.0)
            val currentFiles = "%,d".format(currentStat.files)
            val totalFiles = "%,d".format(expectedStat.files)

            val prcBytes = "%.2f".format(this.progressBytes * 100.0)
            val currentBytes = currentStat.bytes.readableFileSize()
            val totalBytes = expectedStat.bytes.readableFileSize()

            return "data: $prcBytes% ($currentBytes / $totalBytes)  files: $prcFiles% ($currentFiles / $totalFiles)"
        }
    }

    data class Stat(var files: Int = 0, var bytes: Long = 0)

    class PrintProgress
}
