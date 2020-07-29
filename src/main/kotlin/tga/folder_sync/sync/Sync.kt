package tga.folder_sync.sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */


class Sync(val sessionFolderArg: String?) {

    companion object {
        val logger: Logger = LoggerFactory.getLogger( this::class.java.declaringClass )
    }

    fun perform() {
        val sessionFolder = getSession(sessionFolderArg)
            ?: throw SessionFolderNotFound()

        logger.info("Session folder detected: {}", sessionFolder.absolutePath)

        val planFile = File(sessionFolder.absolutePath + "/plan.txt")


        val planLines = planFile.readLines().toTypedArray()
        val stringSequence = planLines.asSequence()

        var lineNumber = 0
        var srcRoot: String? = null
        var dstRoot: String? = null

        val commandsSequence = stringSequence
            .map {
                logger.trace(it)
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

}

class SpecifiedSessionFolderNotFound(folderName: String) : RuntimeException("can't see the folder '$folderName'")
class SessionFolderNotFound : RuntimeException("No session folder detected. Use 'init' command to generate one.")
