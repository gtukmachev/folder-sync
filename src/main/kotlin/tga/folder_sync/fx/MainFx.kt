package tga.folder_sync.fx

import javafx.application.Application
import javafx.application.Application.launch
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField
import javafx.scene.layout.GridPane
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.Stage


fun main(args: Array<String>) {
    launch(MainFx::class.java, *args)
}

class MainFx : Application() {

    override fun start(primaryStage: Stage) {

        val rootGrid = GridPane().apply{
            alignment = Pos.CENTER
            hgap = 10.0
            vgap = 10.0
            padding = Insets(25.0)
        }

        Text("Welcome").apply { id = "welcome-text" }.also { rootGrid.add( it, 0,0, 2,1 ) }

        Label("User Name:").also { rootGrid.add( it, 0, 1) }
        TextField()        .also { rootGrid.add( it, 1, 1) }
        Label("Password:") .also { rootGrid.add( it, 0, 2) }
        PasswordField()    .also { rootGrid.add( it, 1, 2) }

        val actionTarget = Text().apply { id = "action-target"; rootGrid.add( this, 1, 6) }

        Button("Sign in").apply { rootGrid.add( this, 1, 4) }
            .onAction = EventHandler<ActionEvent> {
                actionTarget.fill = Color.FIREBRICK
                actionTarget.text = "Button pressed"
            }

        primaryStage.apply {
            title = "Sign In"
            scene = Scene(rootGrid, 300.0, 250.0)
            val clz = MainFx::class.java
            print(clz.name)
            val css = clz.getResource("Login.css")
            scene.stylesheets.add( css.toExternalForm() )
        }.show()

    }
}