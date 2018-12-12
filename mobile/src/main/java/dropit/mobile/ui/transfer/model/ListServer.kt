package dropit.mobile.ui.transfer.model

import java.io.Serializable
import java.util.*

class ListServer(
        val name: String,
        val id: UUID,
        val port: Int,
        val ip: String
) : Serializable