package dropit.infrastructure.discovery

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.discovery.DISCOVERY_GROUP
import dropit.application.discovery.DISCOVERY_PORT
import dropit.application.dto.BroadcastMessage
import dropit.application.settings.AppSettings
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.DatagramPacket
import java.net.MulticastSocket

class DiscoveryBroadcaster(appSettings: AppSettings, objectMapper: ObjectMapper) {
    val log = LoggerFactory.getLogger(this::class.java)
    val broadcastPort = DISCOVERY_PORT
    val group = DISCOVERY_GROUP
    val socket = MulticastSocket(broadcastPort)
    private var running = true
    val senderThread = Thread {
        while (running) {
            val settings = appSettings.settings
            val broadcast = BroadcastMessage(settings.computerName, settings.computerId, settings.serverPort)
            val message = objectMapper.writeValueAsBytes(broadcast)
            val packet = DatagramPacket(message, message.size, group, broadcastPort)
            try {
                log.trace("Sending broadcast - $broadcast")
                socket.send(packet)
                @Suppress("MagicNumber")
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

    fun stop() {
        running = false
        senderThread.join()
    }
}