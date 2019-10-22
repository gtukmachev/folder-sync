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
    override val exists:       Boolean get() = file.exists()
    override val isDirectory:  Boolean by lazy { file.isDirectory }
    override val size:            Long by lazy { if (isDirectory) 1 else file.length() }


    override fun relativeTo(base: SFile): String {
        val baseFile = File(base.path)
        val rel = this.file.relativeTo( baseFile )
        return rel.path
    }

    override fun buildTree(ordered: Boolean): Tree<SFile> {
        val root = Tree(this, null)
        var timer = System.currentTimeMillis()
        var counter = 0

        fun addChildren(node: Tree<LocalSFile>) {
            counter++
            if (counter % 1000 == 0 || (System.currentTimeMillis() - timer) > 1000) {
                println("    $counter files scanned")
                timer = System.currentTimeMillis()
            }

            val subfolders = node.obj.file.listFiles()

            if (ordered) subfolders.sort()

            subfolders.forEach { f ->
                val subNode = Tree(LocalSFile(f), node)
                node.children.add(subNode)
                if (f.isDirectory) addChildren(subNode)
            }
        }

        addChildren(root)
        println("    $counter files scanned")

        return root as Tree<SFile>
    }


}