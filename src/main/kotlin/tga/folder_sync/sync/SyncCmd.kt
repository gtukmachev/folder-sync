package tga.folder_sync.sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tga.folder_sync.files.FoldersFactory
import tga.folder_sync.files.LocalSFile
import tga.folder_sync.files.SFile

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */
abstract class SyncCmd(
    val lineNumber: Int,
    var completed: Boolean,
    val fileSize: Int,
    val fileName: String
) {

    abstract fun doAction(): SyncCmd

    companion object {
        val log: Logger = LoggerFactory.getLogger("tga.folder_sync.sync.SyncCmd")

        fun makeCommand(commandLine: String, lineNumber: Int, srcRoot: String?, dstRoot: String?): SyncCmd {
            if (commandLine.trim().startsWith("#")) return SkipCmd(lineNumber) //isComment

            if (srcRoot == null) throw RuntimeException("Wrong plan file format: 'source folder' field not found")
            if (dstRoot == null) throw RuntimeException("Wrong plan file format: 'destination folder' field not found")

            val lexems = commandLine.split("|").map { it.trim() }

            val wasCompleted = lexems[0] == "+"

            val commandLexem = lexems[1]
            val size: Int = try {
                lexems[2].replace(".", "").replace(",", "").toInt()
            } catch (ex: Exception) {
                throw RuntimeException(
                    "Error of parsing income file, in the line #$lineNumber. The second field (file size) unrecognized!",
                    ex
                )
            }

            val cmdObj = when (commandLexem) {
                  "mk <folder>" -> MkDirCmd(lineNumber, wasCompleted, size, dstRoot + '/' + lexems[3])
                 "del <folder>" ->   DelCmd(lineNumber, wasCompleted, size, dstRoot + '/' + lexems[3])
                 "del < file >" ->   DelCmd(lineNumber, wasCompleted, size, dstRoot + '/' + lexems[3])
                "copy < file >" ->  CopyCmd(lineNumber, wasCompleted, size, srcRoot + '/' + lexems[3], dstRoot + '/' + lexems[3])
                else -> SkipCmd(lineNumber)
            }
            return cmdObj
        }

    }

    fun perform(): SyncCmd {
        log.trace("${this::class.simpleName}.perform($lineNumber, $fileSize, $completed, $fileName")

        val res = if (!completed) {
            val r = doAction()
            completed = true
            r
        } else {
            this
        }

        return res
    }

    override fun toString() = "${this::class.simpleName}(lineNumber=$lineNumber, fileName='$fileName', completed=$completed, fileSize=$fileSize)"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SyncCmd) return false

        if (lineNumber != other.lineNumber) return false
        if (completed != other.completed) return false
        if (fileSize != other.fileSize) return false
        if (fileName != other.fileName) return false

        return true
    }
    override fun hashCode(): Int {
        var result = lineNumber
        result = 31 * result + completed.hashCode()
        result = 31 * result + fileSize
        result = 31 * result + fileName.hashCode()
        return result
    }

}

class MkDirCmd(lineNumber: Int, completed: Boolean, fileSize: Int, fileName: String) : SyncCmd(lineNumber, completed, fileSize, fileName) {
    override fun doAction(): MkDirCmd {
        val dstFile: SFile = FoldersFactory.create(fileName)
        dstFile.mkFolder()
        return this
    }
}

class DelCmd(lineNumber: Int, completed: Boolean, fileSize: Int, fileName: String) : SyncCmd(lineNumber, completed, fileSize, fileName) {
    override fun doAction(): DelCmd {
        log.trace("DelCmd.perform($lineNumber, $fileSize, $fileName")
        val file: SFile = FoldersFactory.create(fileName)
        file.removeFile()
        return this
    }
}

class CopyCmd(lineNumber: Int, completed: Boolean, fileSize: Int, fileName: String, val dstFileName: String) : SyncCmd(lineNumber, completed, fileSize, fileName) {
    override fun doAction(): CopyCmd {
        log.trace("CopyCmd.perform($lineNumber, $fileSize, $fileName, $dstFileName")
        val srcFile: SFile = FoldersFactory.create(fileName)
        val dstFile: SFile = FoldersFactory.create(dstFileName)

        dstFile.copyToIt(srcFile as LocalSFile)

        return this
    }
}

class SkipCmd(lineNumber: Int) : SyncCmd(lineNumber, true, 0, "") {
    override fun doAction(): SkipCmd {
        log.trace("SkipCmd.perform($lineNumber")
        return this
    }
}

class UnrecognizedCmd(lineNumber: Int, completed: Boolean, val reason: Throwable) : SyncCmd(lineNumber, completed, 0, "") {
    override fun doAction(): UnrecognizedCmd {
        log.trace("UnrecognizedCmd.perform($lineNumber")
        return this
    }
}
