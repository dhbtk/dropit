package dropit.application

import dropit.application.controllers.*
import dropit.application.model.PhoneRole
import io.javalin.apibuilder.ApiBuilder.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Routes @Inject constructor(
    private val versionController: VersionController,
    private val tokensController: TokensController,
    private val transfersController: TransfersController,
    private val filesController: FilesController,
    private val clipboardsController: ClipboardsController,
    private val downloadsController: DownloadsController
) {
    fun configure() {
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
