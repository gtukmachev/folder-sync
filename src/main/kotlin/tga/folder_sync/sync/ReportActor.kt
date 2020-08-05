package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.japi.pf.ReceiveBuilder
import java.io.File

class ReportActor(val planFile: File, val planLines: Array<String>) : AbstractLoggingActor() {

    data class Done(val result: Pair<SyncCmd, Throwable?>)

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(Done::class.java){ report(it.result) }
        .build()

    private fun report(result: Pair<SyncCmd, Throwable?>) {
        val (cmd, err) = result
        val LN = cmd.lineNumber - 1

        val result_ = when {
            cmd is UnrecognizedCmd -> "err" + planLines[LN].substring(3) + " | " + toSingleStr(cmd.reason)
                       err != null -> "err" + planLines[LN].substring(3) + " | " + toSingleStr(err)
                              else -> " + " + planLines[LN].substring(3)
        }

        planLines[LN] = result_

        saveFile()
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
