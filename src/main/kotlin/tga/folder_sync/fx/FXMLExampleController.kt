package tga.folder_sync.fx

import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.text.Text;

class FXMLExampleController {

    @FXML
    private lateinit var actiontarget: Text

    @FXML
    protected fun handleSubmitButtonAction(event: ActionEvent) {
        actiontarget.text  = "Sign in button pressed"
    }

}