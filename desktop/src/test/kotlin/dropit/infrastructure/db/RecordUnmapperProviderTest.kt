package dropit.infrastructure.db

import dropit.domain.entity.Phone
import dropit.domain.entity.Settings
import dropit.jooq.tables.records.PhoneRecord
import dropit.jooq.tables.records.SettingsRecord
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDateTime
import kotlin.test.assertEquals

object RecordUnmapperProviderTest : Spek({

    describe("converting back into a record") {
        context("when fields are not null") {
            val settings = Settings(
                computerName = "",
                transferFolderName = "",
                rootTransferFolder = ""
            )
            val phone = Phone(createdAt = LocalDateTime.now())
            val settingsRecord = SettingsRecord()
            val phoneRecord = PhoneRecord()

            before {
                EntityBridge.copyToRecord(settings, settingsRecord)
                EntityBridge.copyToRecord(phone, phoneRecord)
            }

            it("converts them all properly") {
                assertEquals(settings.computerId, settingsRecord.computerId)
                assertEquals(settings.computerName, settingsRecord.computerName)
                assertEquals(settings.serverPort, settingsRecord.serverPort)
                assertEquals(true, settingsRecord.logClipboardTransfers)
                assertEquals(settings.showTransferAction, settingsRecord.showTransferAction)
            }
        }
    }
})
