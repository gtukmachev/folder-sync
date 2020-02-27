package tga.folder_sync.fx.model

import java.io.File

enum class Status { toCopy, synchronized }

data class FileItem(val file: File, var status: Status? = null) {

    override fun toString(): String = file.name
}