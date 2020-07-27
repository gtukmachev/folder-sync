package tga.folder_sync.it

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FolderUnit) return false

        if (name != other.name) return false
        if (children != other.children) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + children.hashCode()
        return result
    }

    override fun toString(): String {
        return "FolderUnit(name='$name', children=$children)"
    }

    companion object {
        fun fromFile(rootFolder: String): FolderUnit {
            val rootFile = File(rootFolder)
            if (!rootFile.exists()) throw RuntimeException("The folder is not exists: '$rootFolder'! ")

            val root = FolderUnit(null, ".")

            fun addChildren(node: FolderUnit, nodePath: String) {
                val children = File(nodePath).listFiles()!!.sorted()

                children.forEach {
                    val subNode: FolderUnit = when {
                        it.isDirectory -> FolderUnit(node, it.name)
                                  else -> FileUnit(node, it.name, "")
                    }
                    node.children.add(subNode)
                    if (it.isDirectory) addChildren(subNode, it.path)
                }
            }

            addChildren(root, rootFolder)

            return root
        }
    }

    fun Fld(folderName: String, f: folderContent? = null) {
        val newChild = FolderUnit(this, this.name + "/" + folderName)
        this.children += newChild
        if (f != null) newChild.f()
    }

    fun Txt(fileName: String) {
        val newFile = FileUnit(this, this.name + "/" + fileName, "txt")
        this.children += newFile
    }

    fun clearAndMake(): FolderUnit {
        clear()
        make()
        return this
    }
}

class FileUnit(parent: FolderUnit?, name: String, val type: String) : FolderUnit(parent, name) {
    override fun make() {
        val fullName = "$name.$type"
        File(fullName).appendText("$fullName\nAuto-generated test file")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FileUnit) return false
        if (!super.equals(other)) return false

        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return "FileUnit(type='$type')"
    }


}

fun Fld(folderName: String, f: folderContent? = null): FolderUnit {
    val rootFolder = FolderUnit(null, folderName)
    if (f != null) rootFolder.f()
    return rootFolder
}

fun folderStructure(folderName: String, f: folderContent? = null) = Fld(folderName, f)
    .clearAndMake()
    .name

