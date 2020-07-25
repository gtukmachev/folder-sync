package tga.folder_sync.sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */

private val logger: Logger = LoggerFactory.getLogger("tga.folder_sync.sync.sync")
private val now = Date()

fun sync(sessionFolderArg: String?) {
    val sessionFolder = getSession(sessionFolderArg)
        ?: throw SessionFolderNotFound()

    logger.info("Session folder detected: {}", sessionFolder.absolutePath)

    val planFile = File(sessionFolder.absolutePath + "/plan.txt")

    //planFile.readLines()

    planFile.useLines { stringSequence ->
        var lineNumber = 0
        //val threadsNumber = config.getInt("threads")

        val commandsSequence = stringSequence
            .map { SyncCmd.makeCommand(it, ++lineNumber) }
            .filter { it !is SkipCmd }

        commandsSequence.forEach{ it.perform() }

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

    return subFolders.maxBy { f -> folderNamePattern.parse(f.name) }

}


class SpecifiedSessionFolderNotFound(folderName: String) : RuntimeException("can't see the folder '$folderName'")
class SessionFolderNotFound : RuntimeException("No session folder detected. Use 'init' command to generate one.")
