package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.actor.ActorRef
import akka.actor.OneForOneStrategy
import akka.actor.Props
import akka.japi.pf.DeciderBuilder
import akka.japi.pf.ReceiveBuilder
import akka.routing.RoundRobinPool
import tga.folder_sync.exts.Resume
import java.io.File
import java.text.SimpleDateFormat
import java.time.Duration


/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */
class SyncActor(
    private val sessionFolderArg: String?,
    private val nOfRoutes: Int,
    private val incomeLinesFilter: inFilter?
) : AbstractLoggingActor() {

    class Perform
    data class Done(val result: String) //TODO : the "result" field should be not a simple string, but a statistic object

    lateinit var planFile: File
    lateinit var planLines: Array<String>
    lateinit var reportActor: ActorRef
    lateinit var cmdActor: ActorRef

    var lineNumber = 0
    var srcRoot: String? = null
    var dstRoot: String? = null

    var commandsLaunchedNumber: Int = 0
    var errCount: Int = 0

    lateinit var listener: ActorRef

    override fun createReceive() = ReceiveBuilder()
        .match(Perform::class.java           ) { listener = sender();                     raiseNextCommands(); checkIfDone() }
        .match(ReportActor.Done::class.java  ) { commandsLaunchedNumber--; checkIfDone(); raiseNextCommands(); checkIfDone() }
        .build()

    private val strategy = OneForOneStrategy( 10, Duration.ofSeconds(60),
        DeciderBuilder
            .match( Throwable::class.java, { sender == cmdActor }) { handleErr(it); Resume }
            .build()
    )

    override fun supervisorStrategy() = strategy

    private fun handleErr(err: Throwable) {
        errCount ++
        commandsLaunchedNumber --
        checkIfDone()
        log().error("Unknown $errCount error in cmdActor", err)
        raiseNextCommands()
    }

    override fun preStart() {
        val sessionFolder = getSession(sessionFolderArg)
            ?: throw SessionFolderNotFound()
        log().info("Session folder detected: {}", sessionFolder.absolutePath)

        planFile = File(sessionFolder.absolutePath + "/plan.txt")
        planLines = planFile.readLines().toTypedArray()

        reportActor = context.actorOf( Props.create(ReportActor::class.java, planFile, planLines, self()), "reportActor" )
        cmdActor = context.actorOf(
                RoundRobinPool(nOfRoutes).props(
                    Props.create(CmdActor::class.java, reportActor)
                )
                , "cmdRouter"
        )

        lineNumber = 0
        srcRoot = null
        dstRoot = null
//        commandsLounchedNumber = 0
        errCount = 0
    }

    fun raiseNextCommands() {
        val cmd = getNextCommand()
        log().debug("[raiseNextCommands] commandsLaunchedNumber=$commandsLaunchedNumber, nOfRutees=$nOfRoutes  cmd=${cmd}")

        if (cmd != null) {
            commandsLaunchedNumber++
            cmdActor.tell( cmd, self() )

            if (commandsLaunchedNumber < nOfRoutes) raiseNextCommands()
        }
    }

    private fun checkIfDone() {
        log().debug("[checkIfDone] commandsLaunchedNumber=$commandsLaunchedNumber, lineNumber=$lineNumber, planLines.size=${planLines.size}")
        if (commandsLaunchedNumber == 0 && lineNumber >= planLines.size) {
            listener.tell( Done("OK"), self() )
        }
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

    private fun getNextCommand(): SyncCmd? {
        var cmd = readNextCommand()
        while ( (cmd != null) && (cmd is SkipCmd || cmd.completed) ) cmd = readNextCommand()
        return cmd
    }

    private fun readNextCommand(): SyncCmd? {
        if (lineNumber >= planLines.size) return null
        val line = planLines[lineNumber]
        lineNumber++
        if (line.startsWith("#  -      source folder:")) srcRoot = line.split(": ").get(1)
        if (line.startsWith("#  - destination folder:")) dstRoot = line.split(": ").get(1)
        val cmd = try {
            SyncCmd.makeCommand(line, lineNumber, srcRoot, dstRoot, incomeLinesFilter)
        } catch (t: Throwable) {
            UnrecognizedCmd(lineNumber, line.startsWith("err"), t)
        }
        log().debug(line + " ::: " + cmd)
        return cmd
    }

}

class SpecifiedSessionFolderNotFound(folderName: String) : RuntimeException("can't see the folder '$folderName'")
class SessionFolderNotFound : RuntimeException("No session folder detected. Use 'init' command to generate one.")
