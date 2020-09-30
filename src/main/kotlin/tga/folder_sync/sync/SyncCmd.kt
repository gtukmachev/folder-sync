package tga.folder_sync.sync

import akka.event.LoggingAdapter
import tga.folder_sync.files.FoldersFactory
import tga.folder_sync.files.LocalSFile
import tga.folder_sync.files.SFile

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */
typealias inFilter = (String) -> Boolean

abstract class SyncCmd(
    val lineNumber: Int,
    var completed: Boolean,
    val fileSize: Int,
    val fileName: String
) {

    abstract fun doAction(logger: LoggingAdapter): SyncCmd

    companion object {
        fun makeCommand(commandLine: String, lineNumber: Int, srcRoot: String?, dstRoot: String?, incomeLinesFilter: inFilter? ): SyncCmd {
            if (commandLine.trim().startsWith("#")) return SkipCmd(lineNumber) //isComment
            if (incomeLinesFilter != null) {
                val check = incomeLinesFilter(commandLine)
                if (!check) return SkipCmd(lineNumber)
            }

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

    fun perform(logger: LoggingAdapter): SyncCmd {
        logger.debug("{}.perform(lineNumber={}, fileSize={}, fileName={})",this::class.simpleName, lineNumber, fileSize, fileName)

        val res = if (!completed) {
            val r = doAction(logger)
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
    override fun doAction(logger: LoggingAdapter): MkDirCmd {
        logger.debug("MkDirCmd.doAction(lineNumber={}, fileSize={}, fileName={})", lineNumber, fileSize, fileName)
        val dstFile: SFile = FoldersFactory.create(fileName)
        dstFile.mkFolder()
        return this
    }
}

class DelCmd(lineNumber: Int, completed: Boolean, fileSize: Int, fileName: String) : SyncCmd(lineNumber, completed, fileSize, fileName) {
    override fun doAction(logger: LoggingAdapter): DelCmd {
        logger.debug("DelCmd.doAction(lineNumber={}, fileSize={}, fileName={})", lineNumber, fileSize, fileName)
        val file: SFile = FoldersFactory.create(fileName)
        file.removeFile()
        return this
    }
}

class CopyCmd(lineNumber: Int, completed: Boolean, fileSize: Int, fileName: String, val dstFileName: String) : SyncCmd(lineNumber, completed, fileSize, fileName) {
    override fun doAction(logger: LoggingAdapter): CopyCmd {
        logger.debug("CopyCmd.doAction(lineNumber={}, fileSize={}, fileName={}, dstFileName={})", lineNumber, fileSize, fileName, dstFileName)
        val srcFile: SFile = FoldersFactory.create(fileName)
        val dstFile: SFile = FoldersFactory.create(dstFileName)

        dstFile.copyToIt(srcFile as LocalSFile, logger)

        return this
    }
}

class SkipCmd(lineNumber: Int) : SyncCmd(lineNumber, true, 0, "") {
    override fun doAction(logger: LoggingAdapter): SkipCmd {
        logger.debug("SkipCmd.doAction(lineNumber={})", lineNumber)
        return this
    }
}

class UnrecognizedCmd(lineNumber: Int, completed: Boolean, val reason: Throwable) : SyncCmd(lineNumber, completed, 0, "") {
    override fun doAction(logger: LoggingAdapter): UnrecognizedCmd {
        logger.debug("UnrecognizedCmd.doAction(lineNumber={})", lineNumber)
        return this
    }
}
