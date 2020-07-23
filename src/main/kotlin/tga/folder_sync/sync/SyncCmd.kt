package tga.folder_sync.sync

import tga.folder_sync.files.FoldersFactory
import tga.folder_sync.files.LocalSFile
import tga.folder_sync.files.SFile

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */
interface SyncCmd {

    val lineNumber: Int
    val fileSize: Int

    companion object {
        val commndLength = "  mk <folder> ".length

        fun makeCommand(commandLine: String, lineNumber: Int): SyncCmd {
            if (commandLine.trim().startsWith("#")) return SkipCmd(lineNumber) //isComment

            val lexems = commandLine.split("|").map{ it.trim() }

            val commandLexem = lexems[0]
            val size: Int = try { lexems[1].toInt() } catch (ex: Exception) {
                throw RuntimeException("Error of parsing income file, in the line #$lineNumber. The second field (file size) unrecognized!", ex)
            }

            val cmdObj =  when(commandLexem) {
                      "mk <folder>" -> MkDirCmd(lineNumber, size, commandLine.substring(commndLength))
                     "del <folder>" ->   DelCmd(lineNumber, size, commandLine.substring(commndLength))
                     "del < file >" ->   DelCmd(lineNumber, size, commandLine.substring(commndLength))
                    "copy < file >" ->   CopyCmd(lineNumber, size, lexems[2], lexems[3])
                    else -> SkipCmd(lineNumber)
                }
            return cmdObj
        }

    }


    fun perform(): SyncCmd
}

data class MkDirCmd(override val lineNumber: Int, override val fileSize: Int, val dstDirName: String) : SyncCmd {
    override fun perform(): MkDirCmd {
        return this
    }
}

data class DelCmd(override val lineNumber: Int, override val fileSize: Int, val dstFileOrFolderName: String) : SyncCmd {
    override fun perform(): DelCmd {
        throw RuntimeException("test")
    }
}

data class CopyCmd(override val lineNumber: Int, override val fileSize: Int, val srcFileName: String, val dstFileName: String) : SyncCmd {
    override fun perform(): CopyCmd {
        val srcFile: SFile = FoldersFactory.create(srcFileName)
        val dstFile: SFile = FoldersFactory.create(dstFileName)

        dstFile.copyToIt(srcFile as LocalSFile)

        return this
    }
}


data class SkipCmd(override val lineNumber: Int) : SyncCmd {

    override val fileSize: Int = 0

    override fun perform(): SkipCmd {
        return this
    }
}
