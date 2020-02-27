package tga.folder_sync.fx.scenes.process

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import tga.folder_sync.fx.model.FileItem
import java.io.File
import java.net.URL
import java.util.*

class Process : Initializable {

    private val folderIcon      = Image(javaClass.getResourceAsStream("folder.png"))
    private val folderEmptyIcon = Image(javaClass.getResourceAsStream("folder-empty.png"))
    private val fileIcon        = Image(javaClass.getResourceAsStream("file.png"))
    private val clockIcon        = Image(javaClass.getResourceAsStream("clock.png"))

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        files.root = TreeItem( FileItem(File("c:/temp")) ).apply {
            isExpanded = true
        }

    }

    @FXML
    lateinit var files: TreeView<FileItem>

    @FXML
    fun loadSrc(event: ActionEvent) {


        fun expandChildren(item: TreeItem<FileItem>) {
            val file: File = item.value.file
            val img: Node?
            val overlay: Node = ImageView(clockIcon)
            val children: List<File>?

            if (file.isDirectory) {
                children = file.listFiles()?.asList()?.sorted()
                img = ImageView( if (children?.isNotEmpty() == true) folderIcon else folderEmptyIcon)
            } else {
                children = null
                img = ImageView(fileIcon)
            }

            item.graphic = Group(img, overlay)

            children?.forEach{ item.children.add( TreeItem(FileItem(it)) ) }

            item.children.forEach{ expandChildren(it) }
        }

        expandChildren(files.root)
    }

}