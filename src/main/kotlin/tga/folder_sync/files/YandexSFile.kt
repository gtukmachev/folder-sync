package tga.folder_sync.files

import com.yandex.disk.rest.Credentials
import com.yandex.disk.rest.json.Resource
import com.yandex.disk.rest.util.ResourcePath
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import tga.folder_sync.tree.Tree
import tga.folder_sync.yandex.YandexFilesSequence

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class YandexSFile(override val name: String, val credentials: Credentials) : SFile() {
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    override fun buildTree(ordered: Boolean): Tree<SFile> {
        val yandexFilesSequence = YandexFilesSequence(credentials)

        val root = Tree(this, null)
        val filterName = name.substring("disk:".length)

        var scannedFilesNumber = 0
        var filterredFilesNumber = 0

        val filesByFolders = yandexFilesSequence
            .dropWhile {
                val isMatch = it.path.path.startsWith(filterName)
                if (!isMatch) {
                    scannedFilesNumber++
                    if (scannedFilesNumber.rem(100) == 0 ) print(".")
                }
                !isMatch
            }
            .takeWhile {
                val isMatch = it.path.path.startsWith(filterName)
                if (isMatch) {
                    filterredFilesNumber++
                    if (filterredFilesNumber.rem(100) == 0 ) print("*")
                }
                isMatch
            }
            .groupBy { it.path }
            .forEach{ path, files -> addToTree(root, path, files)}

        println("$scannedFilesNumber files scanned / $filterredFilesNumber files filterred")

        return root as Tree<SFile>
    }

    private fun addToTree(root: Tree<YandexSFile>, path: ResourcePath, files: List<Resource>) {
/*
        val pathItems = path.path.split("/")

        var treeFolder = root
        for (folderName in pathItems) {
            var child = treeFolder.children.find { it.obj.path }
        }


*/
    }

    override fun relativeTo(base: SFile): String = TODO("not implemented")

    override val absolutePath:  String get() = TODO("not implemented")
    override val path:          String get() = TODO("not implemented")
    override val pathSeparator: String get() = "/"
    override val exists:       Boolean get() = TODO("not implemented")
    override val isDirectory:  Boolean get() = TODO("not implemented")
    override val size:            Long get() = TODO("not implemented")
}

