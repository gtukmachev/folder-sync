package tga.folder_sync.files

import tga.folder_sync.tree.Tree
import java.io.File

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class LocalSFile(val file: File) : SFile() {

    override val name:          String get() = file.name
    override val absolutePath:  String get() = file.absolutePath
    override val path:          String get() = file.path
    override val pathSeparator: String get() = System.getProperty("file.separator")
    override val exists: Boolean get() = file.exists()


    init {
        if (!file.exists()) throw FolderDoNotExistsException(file.path)
        if (!file.isDirectory) throw NotAFolderException(file.path)
    }



    override fun buildTree(ordered: Boolean): Tree<SFile> {
        val root = Tree(this, null)

        fun addChildren(node: Tree<LocalSFile>) {
            val subfolders = node.obj.file.listFiles{ f -> f.isDirectory }

            if (ordered) subfolders.sort()

            subfolders.forEach { f ->
                val subNode = Tree(LocalSFile(f), node)
                node.children.add(subNode)
                addChildren(subNode)
            }
        }

        addChildren(root)

        return root as Tree<SFile>
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalSFile) return false

        if (file != other.file) return false

        return true
    }

}