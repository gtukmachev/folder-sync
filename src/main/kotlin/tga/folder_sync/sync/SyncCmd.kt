package tga.folder_sync.sync

/**
 * Created by grigory@clearscale.net on 2/25/2019.
 */
interface SyncCmd {

    val lineNumber: Int

    companion object {
        val commndLength = "  mk <folder> ".length

        fun makeCommand(cmd: String, lineNumber: Int): SyncCmd =
            if (cmd.length < 15)
                SkipCmd(lineNumber)
            else
                when(cmd.substring(0,commndLength)) {
                "  mk <folder> " -> MkDirCmd(lineNumber, cmd.substring(commndLength))
                " del <folder> " -> DelCmd(lineNumber, cmd.substring(commndLength))
                " del < file > " -> DelCmd(lineNumber, cmd.substring(commndLength))
                "copy < file > " -> cmd.substring(commndLength).split(" --> ").let{ CopyCmd(lineNumber, it[0], it[1]) }
                else -> SkipCmd(lineNumber)
        }
    }

    fun perform(): SyncCmd
}

data class MkDirCmd(override val lineNumber: Int, val dstDirName: String) : SyncCmd {
    override fun perform(): MkDirCmd {
        return this
    }
}

data class DelCmd(override val lineNumber: Int, val dstFileOrFolderName: String) : SyncCmd {
    override fun perform(): DelCmd {
        return this
    }
}

data class CopyCmd(override val lineNumber: Int, val srcFileName: String, val dstFileName: String) : SyncCmd {
    override fun perform(): CopyCmd {
        return this
    }
}


data class SkipCmd(override val lineNumber: Int) : SyncCmd {
    override fun perform(): SkipCmd {
        return this
    }
}
