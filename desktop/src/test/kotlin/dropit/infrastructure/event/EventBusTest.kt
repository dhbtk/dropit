package dropit.infrastructure.event

import org.junit.jupiter.api.Assertions.*
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object EventBusTest : Spek({
    data class TestEvent(override val payload: String) : AppEvent<String>

    val eventBus by memoized { EventBus() }

    describe("#subscribe") {
        it("subscribes a handler and calls it when broadcast") {
            var called = false
            eventBus.subscribe(TestEvent::class) {
                called = true
            }
            eventBus.broadcast(TestEvent("event"))
            assertTrue(called)
        }

        it("calls multiple subscriptions in order") {
            var called1 = false
            var called2 = false
            eventBus.subscribe(TestEvent::class) {
                assertFalse(called1)
                assertFalse(called2)
                called1 = true
            }
            eventBus.subscribe(TestEvent::class) {
                assertTrue(called1)
                assertFalse(called2)
                called2 = true
            }
            eventBus.broadcast(TestEvent("event"))
            assertTrue(called1)
            assertTrue(called2)
        }
    }

    describe("#unsubscribe") {
        it("unsubscribes successfully") {
            val subscription = eventBus.subscribe(TestEvent::class) {
                fail("subscription not removed")
            }
            eventBus.unsubscribe(TestEvent::class, subscription)
            eventBus.broadcast(TestEvent("test"))
        }

        it("preserves other subscriptions correctly") {
            var called = false
            val subscription = eventBus.subscribe(TestEvent::class) {
                fail("subscription not removed")
            }
            eventBus.subscribe(TestEvent::class) {
                called = true
            }
            eventBus.unsubscribe(TestEvent::class, subscription)
            eventBus.broadcast(TestEvent("test"))
            assertTrue(called)
        }
    }

    describe("#broadcast") {
        it("passes along the event correctly") {
            val event = TestEvent("payload")
            eventBus.subscribe(TestEvent::class) {
                assertEquals(event, it)
            }
            eventBus.broadcast(event)
        }
    }
})