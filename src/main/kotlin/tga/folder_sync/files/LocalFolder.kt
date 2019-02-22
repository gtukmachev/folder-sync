package tga.folder_sync.files

import tga.folder_sync.tree.Tree
import java.io.File

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class LocalFolder(val file: File) : Folder {

    override val name: String
        get() = file.name

    init {
        if (!file.isDirectory) throw RuntimeException("'${file.name}' is not a folder!")
    }


    override fun buildTree(ordered: Boolean): Tree<Folder> {
        val root = Tree(this)

        fun addChildren(node: Tree<LocalFolder>) {
            val subfolders = node.obj.file.listFiles{ f -> f.isDirectory }

            if (ordered) subfolders.sort()

            subfolders.forEach { f ->
                val subNode = Tree(LocalFolder(f))
                node.children.add(subNode)
                addChildren(subNode)
            }
        }

        addChildren(root)

        return root as Tree<Folder>
    }

}