package tga.folder_sync.sync

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tga.folder_sync.files.FoldersFactory
import tga.folder_sync.files.LocalSFile
import tga.folder_sync.files.SFile
import tga.folder_sync.sync.SyncCmd.Companion.log

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */
interface SyncCmd {

    val lineNumber: Int
    val fileSize: Int

    companion object {
        val log: Logger = LoggerFactory.getLogger("tga.folder_sync.sync.SyncCmd")

        fun makeCommand(commandLine: String, lineNumber: Int, srcRoot: String?, dstRoot: String?): SyncCmd {
            if (commandLine.trim().startsWith("#")) return SkipCmd(lineNumber) //isComment

            if (srcRoot == null) throw RuntimeException("Wrong plan file format: 'source folder' field not found")
            if (dstRoot == null) throw RuntimeException("Wrong plan file format: 'destination folder' field not found")

            val lexems = commandLine.split("|").map { it.trim() }

            val commandLexem = lexems[0]
            val size: Int = try {
                lexems[1].toInt()
            } catch (ex: Exception) {
                throw RuntimeException(
                    "Error of parsing income file, in the line #$lineNumber. The second field (file size) unrecognized!",
                    ex
                )
            }

            val cmdObj = when (commandLexem) {
                "mk <folder>" -> MkDirCmd(lineNumber, size, dstRoot + '/' + lexems[2])
                "del <folder>" -> DelCmd(lineNumber, size, dstRoot + '/' + lexems[2])
                "del < file >" -> DelCmd(lineNumber, size, dstRoot + '/' + lexems[2])
                "copy < file >" -> CopyCmd(lineNumber, size, srcRoot + '/' + lexems[2], dstRoot + '/' + lexems[2])
                else -> SkipCmd(lineNumber)
            }
            return cmdObj
        }

    }

    fun perform(): SyncCmd
}

data class MkDirCmd(override val lineNumber: Int, override val fileSize: Int, val dstDirName: String) : SyncCmd {
    override fun perform(): MkDirCmd {
        log.trace("MkDirCmd.perform($lineNumber, $fileSize, $dstDirName")
        val dstFile: SFile = FoldersFactory.create(dstDirName)
        dstFile.mkFolder()
        return this
    }
}

data class DelCmd(override val lineNumber: Int, override val fileSize: Int, val dstFileOrFolderName: String) : SyncCmd {
    override fun perform(): DelCmd {
        log.trace("DelCmd.perform($lineNumber, $fileSize, $dstFileOrFolderName")
        val file: SFile = FoldersFactory.create(dstFileOrFolderName)
        file.removeFile()
        return this
    }
}

data class CopyCmd(override val lineNumber: Int, override val fileSize: Int, val srcFileName: String, val dstFileName: String) : SyncCmd {
    override fun perform(): CopyCmd {
        log.trace("CopyCmd.perform($lineNumber, $fileSize, $srcFileName, $dstFileName")
        val srcFile: SFile = FoldersFactory.create(srcFileName)
        val dstFile: SFile = FoldersFactory.create(dstFileName)

        dstFile.copyToIt(srcFile as LocalSFile)

        return this
    }
}

data class SkipCmd(override val lineNumber: Int) : SyncCmd {
    override val fileSize: Int = 0
    override fun perform(): SkipCmd {
        log.trace("SkipCmd.perform($lineNumber")
        return this
    }
}
