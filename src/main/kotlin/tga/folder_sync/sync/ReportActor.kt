package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.exts.shortMsg
import java.io.File

class ReportActor(val planFile: File, val planLines: Array<String>, val resultListener: ActorRef) : AbstractLoggingActor() {

    var linesNotPosted = 0
    var lastTimePosted = 0L

    data class Done(val cmd: SyncCmd, val err: Throwable?)
    class Flush

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(Done::class.java){ report(it) }
        .match(Flush::class.java){ saveFile(System.currentTimeMillis()) }
        .build()

    private fun report(result: Done) {
        val LN = result.cmd.lineNumber - 1

        val result_ = when {
            result.cmd is UnrecognizedCmd -> "err" + planLines[LN].substring(3) + " | " + result.cmd.reason.shortMsg()
                       result.err != null -> "err" + planLines[LN].substring(3) + " | " + result.err.shortMsg()
                                     else -> " + " + planLines[LN].substring(3)
        }
        planLines[LN] = result_

        linesNotPosted++
        val now = System.currentTimeMillis()
        if (linesNotPosted >= 100 || now - lastTimePosted > 30_000) { //every 30 sec
            saveFile(now)
        }

        resultListener.tell(result, self())
    }

    private fun saveFile(now: Long) {
        planFile.printWriter().use {out -> planLines.forEach(out::println)}
        linesNotPosted = 0
        lastTimePosted = now
    }

}
