package tga.folder_sync.sync

import akka.actor.AbstractLoggingActor
import akka.japi.pf.ReceiveBuilder
import java.io.File
import java.text.SimpleDateFormat

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */


class SyncActor(val sessionFolderArg: String?) : AbstractLoggingActor() {

    override fun createReceive() = ReceiveBuilder()
            .match(Perform::class.java) {
                perform()
                sender().tell(Done("OK"), self())
            }
        .build()

    fun perform() {
        val sessionFolder = getSession(sessionFolderArg)
            ?: throw SessionFolderNotFound()

        log().info("Session folder detected: {}", sessionFolder.absolutePath)

        val planFile = File(sessionFolder.absolutePath + "/plan.txt")


        val planLines = planFile.readLines().toTypedArray()
        val stringSequence = planLines.asSequence()

        var lineNumber = 0
        var srcRoot: String? = null
        var dstRoot: String? = null

        val commandsSequence = stringSequence
            .map {
                log().debug(it)
                if (it.startsWith("#  -      source folder:")) srcRoot = it.split(": ").get(1)
                if (it.startsWith("#  - destination folder:")) dstRoot = it.split(": ").get(1)
                SyncCmd.makeCommand(it, ++lineNumber, srcRoot, dstRoot)
            }

        commandsSequence
            .filter { (it !is SkipCmd) && (!it.completed) }
            .forEach{ cmd ->
                    cmd.perform()
                    markAsComplete(planFile, planLines, cmd)
            }
    }

    private fun markAsComplete(planFile: File, planLines: Array<String>, cmd: SyncCmd) {
        val LN = cmd.lineNumber - 1
        planLines[LN] = '+' + planLines[LN].substring(1)

        planFile.printWriter().use { out ->
            planLines.forEach( out::println )
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

        val sessionsFolder = System.getProperty("outDir", "")
        val folderNamePattern = SimpleDateFormat("'.sync'-yyyy-MM-dd-HH-mm-ss")

        val subFolders = File(sessionsFolder).listFiles{ f -> f.isDirectory
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
    
}

class SpecifiedSessionFolderNotFound(folderName: String) : RuntimeException("can't see the folder '$folderName'")
class SessionFolderNotFound : RuntimeException("No session folder detected. Use 'init' command to generate one.")
