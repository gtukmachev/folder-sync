package tga.folder_sync.sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.System.exit
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */

private val logger: Logger = LoggerFactory.getLogger("tga.folder_sync.sync.sync")
private val now = Date()

fun sync(vararg args: String) {
    val sessionArg: String? = if (args.size > 1) args[1] else null
    val sessionFolder = getSession(sessionArg)
        ?: throw SessionFolderNotFound()

    logger.info("Session folder detected: {}", sessionFolder.absolutePath)

    val planFile = File(sessionFolder.absolutePath + "/plan.txt")

    //planFile.readLines()

    planFile.useLines { stringSequence ->
        var lineNumber = 0

        val planActor = MasterActor(
                    stringSequence.map { SyncCmd.makeCommand(it, ++lineNumber) }
                                  .filter { it !is SkipCmd }
        )

        planActor
            .performAsync()
            .handleAsync( ::quit )

        logger.info("The synchronization process started")
        //todo: add initial state report here
        logger.info("Type Q or X and press <Enter> to stop execution")
         
        val exitCommands = setOf("Q", "q", "X", "x")
        while ( !exitCommands.contains(readLine()) ) {  }

        logger.warn("Execution was interrupted by a user.")
        planActor.stop()

    }

}

fun quit(result: Unit, error: Throwable?){
    if (error != null) {
        logger.error("The program finished with the error: {}", error)
        exit(-1)
    }

    exit(0)
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
