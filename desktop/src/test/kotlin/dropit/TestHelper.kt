package dropit

import dropit.jooq.Tables.*
import org.jooq.DSLContext

object TestHelper {
    fun createComponent(): ApplicationComponent {
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