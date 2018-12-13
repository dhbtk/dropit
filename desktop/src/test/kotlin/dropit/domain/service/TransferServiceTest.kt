package dropit.domain.service

import dropit.TestHelper
import dropit.factories.PhoneFactory
import dropit.factories.TransferFactory
import org.junit.jupiter.api.Assertions.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object TransferServiceTest : Spek({
    val component = TestHelper.createComponent()

    val transferService by memoized { component.transferService() }
    val eventBus by memoized { component.eventBus() }

    beforeEachTest {
        TestHelper.clearDatabase(component.jooq())
    }

    describe("#uploadFile") {
        val phone by memoized { PhoneFactory.authorizedPhone(component.jooq()) }
        val transferRequest by memoized { TransferFactory.transferRequestBinary() }

        it("creates a transfer and uploads the file successfully") {
            val transferId = transferService.createTransfer(phone, transferRequest)

            var beginEventCalled = false
            var progressEventCalled = false
            var fileCompletedEventCalled = false
            var transferCompletedEventCalled = false
            eventBus.subscribe(TransferService.FileTransferBeginEvent::class) { (transferFile) ->
                beginEventCalled = true
                assertEquals(transferRequest.files[0].id, transferFile.id!!.toString())
            }
            eventBus.subscribe(TransferService.FileTransferReceiveEvent::class) { (pair) ->
                val (transferFile, _) = pair
                progressEventCalled = true
                assertEquals(transferRequest.files[0].id, transferFile.id!!.toString())
            }
            eventBus.subscribe(TransferService.FileTransferFinishEvent::class) { (payload) ->
                val (transferFile, folder, file) = payload
                fileCompletedEventCalled = true
                assertEquals(transferRequest.files[0].id, transferFile.id!!.toString())
                assertTrue(folder.isDirectory && folder.exists())
                assertTrue(file.isFile && file.exists())
            }
            eventBus.subscribe(TransferService.TransferCompleteEvent::class) { (payload) ->
                val (transfer, folder, locations) = payload
                transferCompletedEventCalled = true
                assertEquals(1, transfer.files.size)
                assertTrue(folder.isDirectory && folder.exists())
                val file = locations[transfer.files[0].id]
                assertNotNull(file)
                assertTrue(file!!.isFile && file.exists())
            }

            transferService.uploadFile(phone, transferRequest.files[0].id!!, javaClass.getResourceAsStream("/zeroes.bin"))

            assertTrue(beginEventCalled)
            assertTrue(progressEventCalled)
            assertTrue(fileCompletedEventCalled)
            assertTrue(transferCompletedEventCalled)
        }
    }
})