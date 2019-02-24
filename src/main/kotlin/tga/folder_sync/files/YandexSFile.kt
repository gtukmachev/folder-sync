package tga.folder_sync.files

import tga.folder_sync.tree.Tree

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class YandexSFile(override val name: String) : SFile() {

    override fun buildTree(ordered: Boolean): Tree<SFile> = TODO("not implemented")

    override val absolutePath:  String get() = TODO("not implemented")
    override val path:          String get() = TODO("not implemented")
    override val pathSeparator: String get() = "/"
    override val exists:      Boolean get() = TODO("not implemented")
    override val isDirectory: Boolean get() = TODO("not implemented")

}