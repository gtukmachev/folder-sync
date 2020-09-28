package tga.folder_sync.files

import tga.folder_sync.tree.Tree

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
abstract class SFile : Comparable<SFile> {

    abstract val protocol: String
    abstract val name: String
    abstract val absolutePath: String
    abstract val path: String
    abstract val pathSeparator: String
    abstract val exists: Boolean
    abstract val isDirectory: Boolean
    abstract val size: Long

    abstract fun relativeTo(base: SFile): String

    /**
     * List of all files and folders ionside this one
     */
    abstract fun children(): List<SFile>

    override fun compareTo(other: SFile) = when {
         this.isDirectory && !other.isDirectory ->  1
        !this.isDirectory &&  other.isDirectory -> -1
         else -> this.name.compareTo( other.name, ignoreCase = true )
        // todo: Add comparing of file size, file modification date
    }

    override fun toString() = path

    /**
     * <p>The function builds and returns full folders tree which contains all sub-folders.</p>
     *
     * @param ordered defines if the tree will be ordered or not. True by default.
     *                "ordered" means - all sub-folders inside a parent folder will be ordered by alphabet
     *
     */
    fun buildTree(ordered: Boolean = true): Tree<SFile> {
        if (!this.exists) throw RuntimeException("The file/folder is not exists: '${this.path}'! ")

        val root: Tree<SFile> = Tree(this, null)
        var timer = System.currentTimeMillis()
        var filesCounter = 0

        fun addChildren(node: Tree<SFile>) {

            val subfolders = node.obj.children()
            if (ordered) subfolders.sorted()

            subfolders.forEach {
                val subNode = Tree(it, node)
                node.children.add(subNode)
                filesCounter++
                if (filesCounter % 100 == 0 || (System.currentTimeMillis() - timer) > 1000) {
                    println("    $filesCounter files scanned")
                    timer = System.currentTimeMillis()
                }
                if (it.isDirectory) addChildren(subNode)
            }
        }

        println(" ${root.obj.protocol}${root.obj.absolutePath} scanning:")
        addChildren(root)
        println("    $filesCounter files scanned")

        return root
    }

    abstract fun copyToIt(srcFile: LocalSFile)
    abstract fun mkFolder()
    abstract fun removeFile()

}
