package tga.folder_sync.fx.model

import tga.folder_sync.files.SFile
import tga.folder_sync.tree.Tree

object Context {

    lateinit var srcTree: Tree<SFile>
    lateinit var dstTree: Tree<SFile>

}