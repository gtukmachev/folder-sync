package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.Props
import akka.japi.pf.ReceiveBuilder
import java.io.File
import java.text.SimpleDateFormat

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */


class SyncActor(val sessionFolderArg: String?) : AbstractLoggingActor() {

    lateinit var planFile: File
    lateinit var planLines: Array<String>
    lateinit var commandsSequence: Sequence<SyncCmd>

    lateinit var reportActor: ActorRef
    lateinit var cmdActor: ActorRef
    var restOfResults: Int = 0

    lateinit var listener: ActorRef

    override fun createReceive() = ReceiveBuilder()
            .match(Perform::class.java           ) { listener = sender(); perform() }
            .match(ReportActor.Done::class.java  ) { checkIfDone(it) }
        .build()

    override fun preStart() {
        val sessionFolder = getSession(sessionFolderArg)
            ?: throw SessionFolderNotFound()
        log().info("Session folder detected: {}", sessionFolder.absolutePath)

        planFile = File(sessionFolder.absolutePath + "/plan.txt")
        planLines = planFile.readLines().toTypedArray()

        reportActor = context.actorOf( Props.create(ReportActor::class.java, planFile, planLines, self()), "reportActor" )
        cmdActor = context.actorOf( Props.create(CmdActor::class.java, reportActor), "cmdActor" )

        commandsSequence = buildCommandsSequence(planLines)
        restOfResults = 0
    }

    fun perform() {
        commandsSequence.forEach{
            restOfResults++
            cmdActor.tell(it, self())
        }
    }

    private fun checkIfDone(it: ReportActor.Done) {
        restOfResults--
        if (restOfResults == 0) listener.tell( Done("OK"), self() )
    }

    private fun getSession(sessionArg: String?): File? {
        if (sessionArg != null) {
            val folder = File(sessionArg)
            if (!folder.exists() || !folder.isDirectory) {
                throw SpecifiedSessionFolderNotFound(sessionArg)
            }
            return folder
        }

        val folderNamePattern = SimpleDateFormat("'.sync'-yyyy-MM-dd-HH-mm-ss")

        val subFolders = File(".").listFiles{ f ->
            if (!f.isDirectory) {
                false
            } else {
                try {
                    folderNamePattern.parse(f.name)
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        return subFolders!!.maxBy { f -> folderNamePattern.parse(f.name) }

    }

    class Perform
    data class Done(val result: String)

    companion object {

        fun buildCommandsSequence(planLines: Array<String>): Sequence<SyncCmd> {
            var lineNumber = 0
            var srcRoot: String? = null
            var dstRoot: String? = null
            return planLines.asSequence()
                .map {
                    if (it.startsWith("#  -      source folder:")) srcRoot = it.split(": ").get(1)
                    if (it.startsWith("#  - destination folder:")) dstRoot = it.split(": ").get(1)
                    lineNumber += 1
                    try {
                        SyncCmd.makeCommand(it, lineNumber, srcRoot, dstRoot)
                    } catch (t: Throwable) {
                        UnrecognizedCmd(lineNumber, it.startsWith("err"), t)
                    }
                }
                .filter { (it !is SkipCmd) && (!it.completed) }

        }

    }
    
}

class SpecifiedSessionFolderNotFound(folderName: String) : RuntimeException("can't see the folder '$folderName'")
class SessionFolderNotFound : RuntimeException("No session folder detected. Use 'init' command to generate one.")
