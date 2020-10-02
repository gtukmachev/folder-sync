package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.japi.pf.ReceiveBuilder
import tga.folder_sync.exts.shortMsg
import java.io.File

class PlanUpdaterActor(
        planFilePath: String,
    val planLines: Array<String>
) : AbstractLoggingActor() {

    val planFile = File(planFilePath)
    var linesNotPosted = 0
    var lastTimePosted = 0L

    data class UpdatePlanLine(val cmd: SyncCmd, val err: Throwable?)
    class Finish
    class ReportUpdaterIsDone

    override fun createReceive(): Receive = ReceiveBuilder()
        .match(UpdatePlanLine::class.java){ report(it) }
        .match(Finish::class.java){ finishWork() }
        .build()

    private fun finishWork() {
        context.become( ReceiveBuilder().build() )
        saveFile(System.currentTimeMillis())
        sender.tell( ReportUpdaterIsDone(), self )
    }

    private fun report(result: UpdatePlanLine) {
        if (result.cmd is SkipCmd) return

        linesNotPosted++
        val lineIndex = result.cmd.lineNumber - 1
        val line = planLines[lineIndex].substring(4)

        val result_ = when {
            result.cmd is UnrecognizedCmd -> "err|$line | " + result.cmd.reason.shortMsg()
                       result.err != null -> "err|$line | " + result.err.shortMsg()
                                     else -> " + |$line"
        }
        planLines[lineIndex] = result_

        val now = System.currentTimeMillis()
        if ((linesNotPosted >= 100) || (linesNotPosted > 0 && (now - lastTimePosted) > 30_000)) { //every 30 sec
            saveFile(now)
        }

    }

    private fun saveFile(now: Long) {
        if (linesNotPosted == 0) return

        planFile.printWriter().use {out -> planLines.forEach(out::println)}
        log().info("Plan file is updated with the current progress")
        linesNotPosted = 0
        lastTimePosted = now
    }

}
