package dropit

import org.junit.BeforeClass

abstract class AbstractIntegrationTest {
    companion object {
        @JvmStatic
        @BeforeClass
        fun init() {
            System.setProperty("dropit.test", "true")
        }
    }
}