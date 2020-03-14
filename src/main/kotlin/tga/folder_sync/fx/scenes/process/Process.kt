package tga.folder_sync.fx.scenes.process

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import tga.folder_sync.files.FoldersFactory
import tga.folder_sync.files.SFile
import tga.folder_sync.fx.model.Context
import tga.folder_sync.tree.Tree

class Process {

    private val folderIcon      = Image(javaClass.getResourceAsStream("folder.png"))
    private val folderEmptyIcon = Image(javaClass.getResourceAsStream("folder-empty.png"))
    private val fileIcon        = Image(javaClass.getResourceAsStream("file.png"))
    private val clockIcon        = Image(javaClass.getResourceAsStream("clock.png"))

    private val srcPath = "c:/temp"
    private val dstPath = "yandex://disk:/temp"

    @FXML lateinit var filesSrcTreeView: TreeView<SFile>
    @FXML lateinit var filesDstTreeView: TreeView<SFile>

    @FXML
    fun loadDst(event: ActionEvent) {
        println("Loading destination folder...")
        Context.dstTree = loadAndVisualizeTree(dstPath, filesDstTreeView)
    }

    @FXML
    fun loadSrc(event: ActionEvent) {
        println("Loading source folder...")
        Context.srcTree = loadAndVisualizeTree(srcPath, filesSrcTreeView)
    }

    private fun loadAndVisualizeTree(path: String, treeView: TreeView<SFile>): Tree<SFile> {
        println("\nScanning folder '$path'...")
        val folder = FoldersFactory.create(path)
        val tree = folder.buildTree()
        visualizeTree( tree, treeView )
        return tree
    }


    private fun visualizeTree(filesTree: Tree<SFile>, treeView: TreeView<SFile>) {
        filesTree.fold(
            {
                sFileTreeItem: Tree<SFile> ->
                    val rootViewTreeItem = TreeItem<SFile>( sFileTreeItem.obj )
                    treeView.root = rootViewTreeItem
                    rootViewTreeItem
            }){
                parentViewItem: TreeItem<SFile>, sFileTreeItem: Tree<SFile> ->
                    val viewTreeItem = TreeItem<SFile>( sFileTreeItem.obj )
                    parentViewItem.children.add( viewTreeItem )
                    viewTreeItem
            }
}

}