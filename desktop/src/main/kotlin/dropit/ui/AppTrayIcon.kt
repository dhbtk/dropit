package dropit.ui

import dropit.APP_NAME
import dropit.Application
import dropit.domain.entity.Phone
import dropit.domain.entity.Transfer
import dropit.infrastructure.i18n.t
import javafx.application.Platform
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO

object AppTrayIcon {
    val trayIcon: TrayIcon?

    init {
        Toolkit.getDefaultToolkit()
        val popUpMenu = PopupMenu()
        val showItem = MenuItem("Show", MenuShortcut(KeyEvent.VK_S, true))
        showItem.addActionListener {
            togglePrimaryStage()
        }
        popUpMenu.add(showItem)
        popUpMenu.addSeparator()
        val exitItem = MenuItem("Exit", MenuShortcut(KeyEvent.VK_E, true))
        exitItem.addActionListener {
            Application.exit()
        }
        popUpMenu.add(exitItem)
        val icon = TrayIcon(
                ImageIO.read(AppTrayIcon::class.java.getResourceAsStream("/ui/icon.png")),
                t("trayIcon.starting", APP_NAME),
                popUpMenu
        )
        icon.isImageAutoSize = true
        SystemTray.getSystemTray().add(icon)
        icon.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                if (e!!.button == MouseEvent.BUTTON1) {
                    togglePrimaryStage()
                }
            }
        })
        trayIcon = icon
    }

    private fun togglePrimaryStage() {
        val stage = Application.primaryStage
        if (stage != null) {
            Platform.runLater {
                if (stage.isShowing) {
                    stage.hide()
                } else {
                    stage.show()
                }
            }
        }
    }

    fun notifyPhoneRequest(phone: Phone) {
        trayIcon?.displayMessage(
                t("appTrayIcon.notifyPhoneRequest.title"),
                t("appTrayIcon.notifyPhoneRequest.message", phone.name!!),
                TrayIcon.MessageType.INFO
        )
    }

    fun notifyTransferStart(transfer: Transfer) {
        trayIcon?.displayMessage(
                t("appTrayIcon.notifyTransferStart.title"),
                t("appTrayIcon.notifyTransferStart.message", transfer.name!!, transfer.phone?.name!!),
                TrayIcon.MessageType.INFO
        )
    }
}