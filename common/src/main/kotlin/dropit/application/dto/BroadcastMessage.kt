package dropit.application.dto

import java.util.*

data class BroadcastMessage(
        val computerName: String,
        val computerId: UUID,
        val port: Int
)