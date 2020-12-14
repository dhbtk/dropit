package dropit.ui.main

import dropit.application.settings.AppSettings
import dropit.logger
import net.glxn.qrgen.core.image.ImageType
import net.glxn.qrgen.javase.QRCode
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import java.io.ByteArrayInputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLEncoder
import kotlin.streams.toList

class PairingQrCode(private val appSettings: AppSettings, private val window: Shell) {
    private val ipAddresses = NetworkInterface.networkInterfaces().map { iface ->
        logger.debug("interface name: ${iface.displayName}, virtual: ${iface.isVirtual}, loopback: ${iface.isLoopback}, is up: ${iface.isUp}")
        iface
    }.filter {
        it.isUp and !it.isLoopback
    }.flatMap { iface ->
        iface.inetAddresses().filter { it is Inet4Address }
    }.map { it.hostAddress }.filter { it != null }.toList()

    private var qrCode: Image = generateQrCode()
    val group = Group(window, SWT.SHADOW_ETCHED_OUT).also { group ->
        group.text = "Pairing"
        group.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
            .apply {
                minimumHeight = 128
            }
        group.layout = GridLayout(1, false).apply {
            marginHeight = 32
            marginWidth = 64
        }
        group.size = window.size
    }
    private val label = Label(group, SWT.CENTER or SWT.WRAP).apply {
        text =
            "Open the DropIt app on your phone, tap \"Scan QR Code\" and\n scan the QR Code below to pair the phone with this computer."
        layoutData = GridData(GridData.FILL_HORIZONTAL).apply {
            minimumHeight = 64
        }
    }
    private val canvas = Label(group, SWT.CENTER).apply {
        layoutData = GridData(SWT.CENTER, SWT.CENTER, true, true).apply {
            widthHint = 384
            heightHint = 384
        }
        image = qrCode
        pack()
    }

    private fun generateQrCode(): Image {
        val params = HashMap<String, String>()
        params["computerName"] = appSettings.computerName
        params["computerId"] = appSettings.computerId.toString()
        params["serverPort"] = appSettings.serverPort.toString()
        params["ipAddress"] = ipAddresses.joinToString(",")
        val encodedParams = params.toList().joinToString("&") { (k, v) -> "${k}=${URLEncoder.encode(v, "UTF-8")}" }
        val broadcast = "dropitapp://pair?${encodedParams}"
        return QRCode.from(broadcast).withSize(256, 256).to(ImageType.PNG).stream()
            .toByteArray().let { ByteArrayInputStream(it) }
            .let { Image(window.display, it) }
    }
}
