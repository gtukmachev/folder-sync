package tga.folder_sync.fx.scenes.process

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import java.io.File
import java.net.URL
import java.util.*

class Process : Initializable {

    private val folderIcon      = Image(javaClass.getResourceAsStream("folder.png"))
    private val folderEmptyIcon = Image(javaClass.getResourceAsStream("folder-empty.png"))
    private val fileIcon        = Image(javaClass.getResourceAsStream("file.png"))

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        files.root = TreeItem(File("c:/temp")).apply {
            isExpanded = true
        }

    }

    @FXML
    lateinit var files: TreeView<File>

    @FXML
    fun showFiles(event: ActionEvent) {

        fun expandChildren(item: TreeItem<File>) {
            val file: File = item.value

            if (file.isDirectory) {

                val children = (file.listFiles()?.asList() ?: listOf()).sorted()
                item.graphic = ImageView( if (children.isNotEmpty()) folderIcon else folderEmptyIcon)
                children.forEach{ item.children.add( TreeItem(it) ) }
                item.children.forEach{ expandChildren(it) }
            } else {
                item.graphic = ImageView(fileIcon)
            }
        }

        expandChildren(files.root)
    }

}