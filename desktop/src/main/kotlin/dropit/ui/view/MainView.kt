package dropit.ui.view

import dropit.ui.view.phone.PhoneListView
import javafx.scene.control.TabPane
import tornadofx.*

class MainView : View() {
    override val root = tabpane {
        tabClosingPolicy = TabPane.TabClosingPolicy.UNAVAILABLE
        prefWidth = 1024.0
        prefHeight = 600.0
        tab("Transfers") {
            vbox {
                label("todo")
            }
        }
        tab("Phone list") {
            this += PhoneListView()
        }
        tab("Settings") {
            label("settings")
        }
    }
}