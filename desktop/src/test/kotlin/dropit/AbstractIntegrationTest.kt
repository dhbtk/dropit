package dropit


abstract class AbstractIntegrationTest {
    companion object {
        @JvmStatic
        fun init() {
            System.setProperty("dropit.test", "true")
        }
    }
}