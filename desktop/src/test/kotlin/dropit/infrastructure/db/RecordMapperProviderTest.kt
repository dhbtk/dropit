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
    describe("mapping String to UUID") {
        val recordMapper = RecordMapperProvider().provide(
            null as RecordType<ClipboardLogRecord>?, ClipboardLog::class.java)
        val record = ClipboardLogRecord()
            .apply {
                id = UUID.randomUUID().toString()
            }
        val entity = recordMapper.map(record)!!
        it("works correctly") {
            assertEquals(UUID.fromString(record.id), entity.id)
        }
    }
    describe("mapping String to Enum") {
        val recordMapper = RecordMapperProvider().provide(
            null as RecordType<ClipboardLogRecord>?, ClipboardLog::class.java)
        context("when string is null") {
            val record = ClipboardLogRecord()
                .apply {
                    source = null
                }
            val entity = recordMapper.map(record)!!
            it("works correctly") {
                assertNull(entity.source)
            }
        }
        context("when string is not null") {
            val record = ClipboardLogRecord()
                .apply {
                    source = "COMPUTER"
                }
            val entity = recordMapper.map(record)!!
            it("works correctly") {
                assertEquals(TransferSource.COMPUTER, entity.source)
            }
        }
    }

    describe("mapping Int to Boolean") {
        val recordMapper = RecordMapperProvider().provide(
            null as RecordType<TransferRecord>?, Transfer::class.java)
        context("when the int is 0") {
            val record = TransferRecord()
                .apply {
                    sendToClipboard = 0
                }
            val entity = recordMapper.map(record)!!
            it("equals false") {
                assertFalse(entity.sendToClipboard!!)
            }
        }

        context("when the int is 1") {
            val record = TransferRecord()
                .apply {
                    sendToClipboard = 1
                }
            val entity = recordMapper.map(record)!!
            it("equals true") {
                assertTrue(entity.sendToClipboard!!)
            }
        }
    }
})
