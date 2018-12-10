package dropit.domain.entity

import java.util.*

data class Settings(
        val computerId: UUID = UUID.randomUUID(),
        val computerName: String,
        val transferFolderName: String,
        val rootTransferFolder: String,
        val serverPort: Int = 58992
)