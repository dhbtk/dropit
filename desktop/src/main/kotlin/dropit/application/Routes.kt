package dropit.application

import dropit.application.controllers.*
import dropit.application.model.PhoneRole
import dropit.jooq.tables.records.PhoneRecord
import dropit.jooq.tables.references.PHONE
import io.javalin.apibuilder.ApiBuilder.*
import io.javalin.http.Context
import org.jooq.DSLContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Routes @Inject constructor(
    private val versionController: VersionController,
    private val tokensController: TokensController,
    private val transfersController: TransfersController,
    private val filesController: FilesController,
    private val clipboardsController: ClipboardsController,
    private val downloadsController: DownloadsController,
    private val jooq: DSLContext
) {
    private fun before(context: Context) {
        val currentPhone: PhoneRecord? = context.currentPhoneUuid().let { id ->
            jooq.selectFrom(PHONE).where(PHONE.TOKEN.eq(id)).fetchOptional().orElse(null)
        }
        context.attribute("currentPhone", currentPhone)
    }

    fun configure() {
        before(::before)
        get("/", versionController::show)
        path("token") {
            post(tokensController::create)
            get(tokensController::show)
        }
        post("transfers", transfersController::create, setOf(PhoneRole.AUTHORIZED))
        post("files/:id", filesController::update, setOf(PhoneRole.AUTHORIZED))
        post("clipboard", clipboardsController::create, setOf(PhoneRole.AUTHORIZED))
        get("downloads/:id", downloadsController::show, setOf(PhoneRole.AUTHORIZED))
    }
}
