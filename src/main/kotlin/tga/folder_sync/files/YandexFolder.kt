package tga.folder_sync.files

import tga.folder_sync.tree.Tree

/**
 * Created by grigory@clearscale.net on 2/21/2019.
 */
class YandexFolder(override val name: String) : Folder() {

    override fun buildTree(ordered: Boolean): Tree<Folder> = TODO("not implemented")

    override val absolutePath:  String get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val path:          String get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val pathSeparator: String get() = "/"
}