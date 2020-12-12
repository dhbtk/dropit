package dropit.infrastructure.discovery

import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.discovery.DISCOVERY_GROUP
import dropit.application.discovery.DISCOVERY_PORT
import dropit.application.dto.BroadcastMessage
import dropit.application.settings.AppSettings
import dropit.infrastructure.NeedsStart
import dropit.infrastructure.NeedsStop
import dropit.logger
import java.io.IOException
import java.net.DatagramPacket
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.streams.toList

@Singleton
class DiscoveryBroadcaster @Inject constructor(
    private val appSettings: AppSettings,
    private val objectMapper: ObjectMapper
) : NeedsStart, NeedsStop {
    private val sockets = NetworkInterface
        .networkInterfaces().filter { it.isUp && !it.isLoopback }
        .map {
            MulticastSocket(DISCOVERY_PORT).apply {
                joinGroup(InetSocketAddress(DISCOVERY_GROUP, DISCOVERY_PORT), it)
            }
        }.toList()
    private var running = true
    private val senderThread = Thread(Runnable(::broadcastLoop))

    override fun start() {
        logger.info("Starting discovery broadcaster on port $DISCOVERY_PORT")
        senderThread.start()
    }

    override fun stop() {
        running = false
        senderThread.join()
    }

    private fun broadcastLoop() {
        while (running) {
            val broadcast = BroadcastMessage(appSettings.computerName, appSettings.computerId, appSettings.serverPort)
            val message = objectMapper.writeValueAsBytes(broadcast)
            val packet = DatagramPacket(message, message.size, DISCOVERY_GROUP, DISCOVERY_PORT)
            try {
                logger.trace("Sending broadcast - $broadcast")
                sockets.forEach { socket -> socket.send(packet) }
                @Suppress("MagicNumber")
                Thread.sleep(1000)
            } catch (e: IOException) {
                logger.warn("Could not send broadcast packet")
                e.printStackTrace()
            }
        }
    }
}
