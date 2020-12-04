package dropit.infrastructure.db

import dropit.domain.entity.ClipboardLog
import dropit.domain.entity.Transfer
import dropit.domain.entity.TransferFile
import dropit.domain.entity.TransferSource
import dropit.jooq.tables.records.ClipboardLogRecord
import dropit.jooq.tables.records.TransferFileRecord
import dropit.jooq.tables.records.TransferRecord
import org.jooq.RecordType
import org.junit.jupiter.api.Assertions.assertTrue
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

object RecordMapperProviderTest : Spek({
    describe("mapping same type") {
        val recordMapper = RecordMapperProvider().provide(
            null as RecordType<ClipboardLogRecord>?, ClipboardLog::class.java)
        val record = ClipboardLogRecord()
            .apply {
                content = "content"
            }
        val entity = recordMapper.map(record)!!
        it("works correctly") {
            assertEquals(record.content, entity.content)
        }
    }
    describe("mapping equivalent type") {
        val recordMapper = RecordMapperProvider().provide(
            null as RecordType<TransferFileRecord>?, TransferFile::class.java)
        val record = TransferFileRecord()
            .apply {
                fileSize = 500
            }
        val entity = recordMapper.map(record)!!
        it("works correctly") {
            assertEquals(record.fileSize, entity.fileSize)
        }
    }
})
