package app

import scalafx.application.JFXApp3
import scalafx.application.JFXApp3.PrimaryStage
import scalafx.scene.Scene
import scalafx.stage.StageStyle
import ui.MainView

object Main extends JFXApp3:

  override def start(): Unit =
    stage = new PrimaryStage:
      title = "Sorting Algorithm Visualizer"
      width = 1280
      height = 780
      minWidth = 900
      minHeight = 600
      scene = new Scene:
        root = MainView()
      initStyle(StageStyle.Decorated)
    stage.show()