package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.japi.pf.ReceiveBuilder
import java.io.File

class ReportActor(val planFile: File, val planLines: Array<String>, val resultListener: ActorRef) : AbstractLoggingActor() {

    data class Done(val cmd: SyncCmd, val err: Throwable?)

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(Done::class.java){ report(it) }
        .build()

    private fun report(result: Done) {
        val LN = result.cmd.lineNumber - 1

        val result_ = when {
            result.cmd is UnrecognizedCmd -> "err" + planLines[LN].substring(3) + " | " + toSingleStr(result.cmd.reason)
                       result.err != null -> "err" + planLines[LN].substring(3) + " | " + toSingleStr(result.err)
                                     else -> " + " + planLines[LN].substring(3)
        }
        planLines[LN] = result_
        saveFile()

        resultListener.tell(result, self())
    }

    private fun saveFile() {
        planFile.printWriter().use {out -> planLines.forEach(out::println)}
    }

    private fun toSingleStr(err: Throwable): String {
        val errClass = err::class.java.simpleName
        val errMsg = err.message
        return "$errClass: \"$errMsg\""
    }

}
