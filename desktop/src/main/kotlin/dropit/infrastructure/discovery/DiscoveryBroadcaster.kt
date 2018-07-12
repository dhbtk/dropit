package dropit.infrastructure.discovery

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.dto.BroadcastMessage
import dropit.application.settings.AppSettings
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.util.logging.Logger

@Component
@Profile("!test")
class DiscoveryBroadcaster(@Value("\${server.port}") serverPort: Int, appSettings: AppSettings, objectMapper: ObjectMapper) {
    val log = Logger.getLogger(this::class.java.name)!!
    final val broadcastPort = 58993
    final val group = InetAddress.getByName("237.0.0.0")
    final val socket = MulticastSocket(broadcastPort)
    final val senderThread = Thread {
        while (true) {
            val message = objectMapper.writeValueAsBytes(BroadcastMessage(appSettings.settings.computerName, appSettings.settings.computerId, serverPort))
            val packet = DatagramPacket(message, message.size, group, broadcastPort)
            try {
                socket.send(packet)
                Thread.sleep(1000)
            } catch (e: IOException) {
                log.warning("Could not send broadcast packet")
                e.printStackTrace()
            }
        }
    }

    init {
        socket.joinGroup(group)
        senderThread.start()
    }
}