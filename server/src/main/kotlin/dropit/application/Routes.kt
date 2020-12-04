package dropit.application

import dropit.application.controllers.TokensController
import dropit.application.controllers.VersionController
import dropit.application.dto.TransferRequest
import dropit.domain.entity.Phone
import io.javalin.apibuilder.ApiBuilder.*
import org.eclipse.jetty.http.HttpStatus
import java.nio.file.Files
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Routes @Inject constructor(
    private val versionController: VersionController,
    private val tokensController: TokensController
) {
    fun configure() {
        get("/", versionController::show)
        path("token") {
            post(tokensController::create)
            get(tokensController::show)
        }
    }
}
