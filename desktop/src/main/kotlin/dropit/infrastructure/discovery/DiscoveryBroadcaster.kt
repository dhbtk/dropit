package dropit.infrastructure.discovery

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.BroadcastMessage
import dropit.application.settings.AppSettings
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

class DiscoveryBroadcaster(serverPort: Int, appSettings: AppSettings, objectMapper: ObjectMapper) {
    val log = LoggerFactory.getLogger(this::class.java)
    val broadcastPort = 58993
    val group = InetAddress.getByName("237.0.0.0")
    val socket = MulticastSocket(broadcastPort)
    val senderThread = Thread {
        while (true) {
            val settings = appSettings.settings
            val broadcast = BroadcastMessage(settings.computerName, settings.computerId, settings.serverPort)
            val message = objectMapper.writeValueAsBytes(broadcast)
            val packet = DatagramPacket(message, message.size, group, broadcastPort)
            try {
                log.trace("Sending broadcast - $broadcast")
                socket.send(packet)
                Thread.sleep(1000)
            } catch (e: IOException) {
                log.warn("Could not send broadcast packet")
                e.printStackTrace()
            }
        }
    }

    init {
        log.info("Starting discovery broadcaster on port $broadcastPort")
        socket.joinGroup(group)
        senderThread.start()
    }
}