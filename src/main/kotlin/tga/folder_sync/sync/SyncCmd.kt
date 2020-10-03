package tga.folder_sync.sync

import akka.event.LoggingAdapter
import tga.folder_sync.files.FoldersFactory
import tga.folder_sync.files.LocalSFile
import tga.folder_sync.files.SFile

typealias inFilter = (String) -> Boolean

abstract class SyncCmd(
    val lineNumber: Int,
    var completed: Boolean,
    val fileSize: Int,
    val fileName: String
) {

    abstract fun doAction(logger: LoggingAdapter)

    companion object {
        fun makeCommand(commandLine: String, lineNumber: Int, srcRoot: String?, dstRoot: String?): SyncCmd {
            try {
                val trimmed = commandLine.trim()
                if (trimmed.startsWith("#") || trimmed.isBlank() ) return SkipCmd(lineNumber) //isComment

                val lexems = commandLine.split("|").map { it.trim() }

                val wasCompleted = lexems[0] == "+"

                val commandLexem = lexems[1]
                val size: Int = try {
                    lexems[2].replace(".", "").replace(",", "").toInt()
                } catch (ex: Exception) {
                    return UnrecognizedCmd(lineNumber, wasCompleted,
                        RuntimeException(
                            "Error of parsing income file, in the line #$lineNumber. The second field (file size) unrecognized!",
                            ex
                        )
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
            } catch (t: Throwable) {
                return UnrecognizedCmd(lineNumber, false, t)
            }
        }
    }

    fun perform(logger: LoggingAdapter) {
        if (!completed) doAction(logger)
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
    override fun doAction(logger: LoggingAdapter) {
        logger.debug("MkDirCmd.doAction(lineNumber={}, fileSize={}, fileName={})", lineNumber, fileSize, fileName)
        val dstFile: SFile = FoldersFactory.create(fileName)
        dstFile.mkFolder()
    }
}

class DelCmd(lineNumber: Int, completed: Boolean, fileSize: Int, fileName: String) : SyncCmd(lineNumber, completed, fileSize, fileName) {
    override fun doAction(logger: LoggingAdapter) {
        logger.debug("DelCmd.doAction(lineNumber={}, fileSize={}, fileName={})", lineNumber, fileSize, fileName)
        val file: SFile = FoldersFactory.create(fileName)
        file.removeFile()
    }
}

class CopyCmd(lineNumber: Int, completed: Boolean, fileSize: Int, fileName: String, val dstFileName: String) : SyncCmd(lineNumber, completed, fileSize, fileName) {
    override fun doAction(logger: LoggingAdapter) {
        logger.debug("CopyCmd.doAction(lineNumber={}, fileSize={}, fileName={}, dstFileName={})", lineNumber, fileSize, fileName, dstFileName)
        val srcFile: SFile = FoldersFactory.create(fileName)
        val dstFile: SFile = FoldersFactory.create(dstFileName)

        dstFile.copyToIt(srcFile as LocalSFile, logger)
    }
}

class SkipCmd(lineNumber: Int) : SyncCmd(lineNumber, true, 0, "") {
    override fun doAction(logger: LoggingAdapter)  {
        logger.debug("SkipCmd.doAction(lineNumber={})", lineNumber)
    }
}

class UnrecognizedCmd(lineNumber: Int, completed: Boolean, val reason: Throwable) : SyncCmd(lineNumber, completed, 0, "") {
    override fun doAction(logger: LoggingAdapter){
        logger.debug("UnrecognizedCmd.doAction(lineNumber={})", lineNumber)
    }
}
