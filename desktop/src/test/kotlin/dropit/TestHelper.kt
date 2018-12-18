package dropit

import dropit.jooq.Tables.*
import org.jooq.DSLContext
import org.slf4j.bridge.SLF4JBridgeHandler

object TestHelper {
    fun createComponent(): ApplicationComponent {
        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()
        System.setProperty("dropit.test", "true")
        return DaggerApplicationComponent.create()
    }

    fun clearDatabase(jooq: DSLContext) {
        jooq.deleteFrom(TRANSFER_FILE).execute()
        jooq.deleteFrom(TRANSFER).execute()
        jooq.deleteFrom(PHONE).execute()
        jooq.deleteFrom(SETTINGS).execute()
    }
}