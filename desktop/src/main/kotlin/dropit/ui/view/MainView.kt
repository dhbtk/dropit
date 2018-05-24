package dropit.ui.view

import dropit.APP_NAME
import dropit.ui.view.phone.PhoneListView
import dropit.ui.view.settings.SettingsView
import dropit.ui.view.transfer.TransferView
import javafx.geometry.Orientation
import javafx.geometry.Side
import javafx.scene.layout.VBox
import tornadofx.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

class MainView : View(APP_NAME) {
    var currentView: View? = null
    override val root = vbox {
        prefWidth = 1024.0
        prefHeight = 600.0
        listmenu(orientation = Orientation.HORIZONTAL, iconPosition = Side.TOP) {
            val list = this
            prefWidthProperty().bind((parent as VBox).widthProperty())
            item(text = "Transfers") {
                prefWidthProperty().bind(list.widthProperty().divide(3))
                activeItem = this
                whenSelected {
                    transitionTo(TransferView::class)
                }
            }
            item(text = "Phones") {
                prefWidthProperty().bind(list.widthProperty().divide(3))
                whenSelected {
                    transitionTo(PhoneListView::class)
                }
            }
            item(text = "Settings") {
                prefWidthProperty().bind(list.widthProperty().divide(3))
                whenSelected {
                    transitionTo(SettingsView::class)
                }
            }
        }
        currentView = TransferView()
        this += currentView!!
    }

    private fun transitionTo(viewClass: KClass<out View>) {
        val view = currentView
        val instance = viewClass.createInstance()
        if (view != null) {
            view.replaceWith(instance)
        } else {
            root.add(instance)
        }
        currentView = instance
    }
}