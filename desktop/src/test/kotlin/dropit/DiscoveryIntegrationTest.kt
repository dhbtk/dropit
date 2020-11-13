package dropit

import dropit.application.discovery.DiscoveryClient
import org.junit.jupiter.api.fail
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

object DiscoveryIntegrationTest : Spek({
    val component = TestHelper.createComponent()
    val eventBus by memoized { component.eventBus() }
    val discoveryBroadcaster by memoized { component.discoveryBroadcaster() }
    val discoveryClient by memoized { DiscoveryClient(component.objectMapper(), eventBus) }

    describe("DiscoveryBroadcaster and DiscoveryClient") {
        it("broadcasts and receives correctly") {
            discoveryBroadcaster.toString()
            var client: DiscoveryClient.ServerBroadcast? = null
            eventBus.subscribe(DiscoveryClient.DiscoveryEvent::class) { (data) -> client = data }
            var tries = 0
            while (true) {
                if (tries > 3) {
                    discoveryBroadcaster.stop()
                    discoveryClient.stop()
                    fail("Did not receive message")
                } else {
                    if (client == null) {
                        tries += 1
                        Thread.sleep(1000)
                    } else {
                        break
                    }
                }
            }
            discoveryBroadcaster.stop()
            discoveryClient.stop()
        }
    }

    afterEachTest {
        discoveryBroadcaster.stop()
        discoveryClient.stop()
    }
})
