package tga.folder_sync

import java.io.File

typealias folderContent = FolderUnit.() -> Unit
open class FolderUnit(val parent: FolderUnit?, val name: String) {
    val children = mutableListOf<FolderUnit>()

    fun clear(){
        File(name).deleteRecursively()
    }

    open fun make() {
        File(name).mkdirs()
        children.forEach { it.make() }
    }
}

class FileUnit(parent: FolderUnit?, name: String, val type: String) : FolderUnit(parent, name) {
    override fun make() {
        val fullName = "$name.$type"
        File(fullName).appendText("$fullName\nAuto-generated test file")
    }
}

fun Fld(folderName: String, f: folderContent? = null): FolderUnit {
    val rootFolder = FolderUnit(null, folderName)
    if (f != null) rootFolder.f()
    return rootFolder
}

fun FolderUnit.Fld(folderName: String, f: folderContent? = null) {
    val newChild = FolderUnit(this, this.name + "/" + folderName)
    this.children += newChild
    if (f != null) newChild.f()
}

fun FolderUnit.Txt(fileName: String) {
    val newFile = FileUnit(this, this.name + "/" + fileName, "txt")
    this.children += newFile
}
