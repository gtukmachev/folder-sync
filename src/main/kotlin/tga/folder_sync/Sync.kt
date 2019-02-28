package tga.folder_sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */

private val logger: Logger = LoggerFactory.getLogger("tga.folder_sync.sync")
private val now = Date()

fun sync(args: Array<String>) {
    val sessionArg: String? = if (args.size > 1) args[1] else null
    val sessionFolder = getSession( sessionArg ) ?: throw SessionFolderNotFound()

    println("Session folder detected: " + sessionFolder.absolutePath)


}

private fun getSession(sessionArg: String?): File? {

    if (sessionArg != null) {
        val folder = File(sessionArg)
        if (!folder.exists() || !folder.isDirectory) {
            throw SpecifiedSessionFolderNotFound(sessionArg)
        }
        return folder
    }

    val sessionsFolder = System.getProperty("outDir", ".")
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