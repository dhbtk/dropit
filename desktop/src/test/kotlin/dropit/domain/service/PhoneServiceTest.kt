package dropit.domain.service

import dropit.TestHelper
import dropit.application.dto.TokenRequest
import dropit.application.dto.TokenStatus
import dropit.factories.PhoneFactory
import org.junit.jupiter.api.Assertions.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.util.*

object PhoneServiceTest : Spek({
    val component = TestHelper.createComponent()

    val phoneService by memoized { component.phoneService() }
    val eventBus by memoized { component.eventBus() }

    beforeEachTest {
        TestHelper.clearDatabase(component.jooq())
    }

    describe("#requestToken") {
        it("creates a token for a given request and it is in 'pending' status") {
            var called = false
            eventBus.subscribe(PhoneService.NewPhoneRequestEvent::class) {
                called = true
                assertEquals(TokenStatus.PENDING, it.payload.status)
            }
            val request = TokenRequest(UUID.randomUUID().toString(), "test phone")
            val token = phoneService.requestToken(request)
            assertNotNull(token)
            assertTrue(called, "NewPhoneRequestEvent has not been raised")
        }
    }

    describe("#getTokenStatus") {
        beforeEach {
            PhoneFactory.multiplePhones(component.jooq())
        }

        it("throws an exception if it can't find a phone") {
            assertThrows(UnauthorizedException::class.java) {
                phoneService.getTokenStatus("abcd")
            }
        }

        it("returns authorized for an authorized phone") {
            assertEquals(TokenStatus.AUTHORIZED, phoneService.getTokenStatus(PhoneFactory.authorizedPhone().token!!.toString()).status)
        }

        it("returns pending for a pending phone") {
            assertEquals(TokenStatus.PENDING, phoneService.getTokenStatus(PhoneFactory.pendingPhone().token!!.toString()).status)
        }

        it("returns denied for a denied phone") {
            assertEquals(TokenStatus.DENIED, phoneService.getTokenStatus(PhoneFactory.deniedPhone().token!!.toString()).status)
        }
    }

    describe("#authorizePhone") {
        context("when the phone exists") {
            val phone by memoized { PhoneFactory.pendingPhone(component.jooq()) }

            it("authorizes the phone") {
                val authorizedPhone = phoneService.authorizePhone(phone.id!!)
                assertEquals(TokenStatus.AUTHORIZED, authorizedPhone.status)
            }
        }

        context("when the phone does not exist") {
            it("raises an exception") {
                assertThrows(RuntimeException::class.java) {
                    phoneService.authorizePhone(UUID.randomUUID())
                }
            }
        }
    }

    describe("#denyPhone") {
        context("when the phone exists") {
            val phone by memoized { PhoneFactory.pendingPhone(component.jooq()) }

            it("denies the phone") {
                val authorizedPhone = phoneService.denyPhone(phone.id!!)
                assertEquals(TokenStatus.DENIED, authorizedPhone.status)
            }
        }

        context("when the phone does not exist") {
            it("raises an exception") {
                assertThrows(RuntimeException::class.java) {
                    phoneService.denyPhone(UUID.randomUUID())
                }
            }
        }
    }

    describe("#listPhones") {
        beforeEach {
            PhoneFactory.multiplePhones(component.jooq())
        }

        context("when showing denied phones") {
            it("shows them all correctly") {
                val phones = phoneService.listPhones(true)
                assertEquals(3, phones.size)
                assertNotNull(phones.find { it.status == TokenStatus.DENIED })
            }
        }

        context("when not showing denied phones") {
            it("shows them correctly") {
                val phones = phoneService.listPhones(false)
                assertEquals(2, phones.size)
                assertNull(phones.find { it.status == TokenStatus.DENIED })
            }
        }
    }

    describe("#deletePhone") {
        context("when the phone is pending") {
            val phone by memoized { PhoneFactory.pendingPhone(component.jooq()) }

            it("deletes the phone successfully") {
                phoneService.deletePhone(phone.id!!)
                assertNull(phoneService.listPhones(true).find { it.id == phone.id })
            }
        }
        context("when the phone is approved") {
            val phone by memoized { PhoneFactory.pendingPhone(component.jooq()) }

            it("deletes the phone successfully") {
                phoneService.deletePhone(phone.id!!)
                assertNull(phoneService.listPhones(true).find { it.id == phone.id })
            }
        }
    }
})