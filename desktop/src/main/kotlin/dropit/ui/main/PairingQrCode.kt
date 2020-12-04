package dropit.ui.main

import dropit.application.settings.AppSettings
import dropit.logger
import net.glxn.qrgen.core.image.ImageType
import net.glxn.qrgen.javase.QRCode
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLEncoder
import kotlin.streams.toList

class PairingQrCode(private val appSettings: AppSettings, private val window: Shell) {
    private var qrCode: Image = generateQrCode()
    private val group = Group(window, SWT.SHADOW_ETCHED_OUT).also { group ->
        group.text = "Pairing QR Code"
        group.layoutData = GridData(GridData.FILL_HORIZONTAL)
            .apply {
                minimumHeight = 128
            }
        org.eclipse.swt.layout.GridLayout(1, false)
            .apply {
                group.layout = this
            }
    }
    private val ipAddresses = NetworkInterface.networkInterfaces().map { iface ->
        logger.debug("interface name: ${iface.displayName}, virtual: ${iface.isVirtual}, loopback: ${iface.isLoopback}, is up: ${iface.isUp}")
        iface
    }.filter {
        it.isUp and !it.isLoopback
    }.flatMap { iface ->
        iface.inetAddresses().filter { it is Inet4Address }
    }.map { it.hostAddress }.toList()
    private val canvas = Label(group, SWT.CENTER).apply {
        layoutData = GridData(SWT.CENTER, SWT.CENTER, true, true).apply {
            widthHint = 256
            heightHint = 256
        }
        image = qrCode
        pack()
    }

    private fun generateQrCode(): Image {
        val params = HashMap<String, String>()
        params["computerName"] = appSettings.settings.computerName
        params["computerId"] = appSettings.settings.computerId.toString()
        params["serverPort"] = appSettings.settings.serverPort.toString()
        params["ipAddress"] = ipAddresses.joinToString(",")
        val encodedParams = params.toList().joinToString("&") { (k, v) -> "${k}=${URLEncoder.encode(v, "UTF-8")}" }
        val broadcast = "dropitapp://pair?${encodedParams}"
        return QRCode.from(broadcast).withSize(256, 256).to(ImageType.PNG).stream()
            .toByteArray().let { ByteArrayInputStream(it) }
            .let { Image(window.display, it) }
    }
}
