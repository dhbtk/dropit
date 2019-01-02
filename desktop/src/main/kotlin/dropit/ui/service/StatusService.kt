package dropit.ui.service

import dropit.application.PhoneSessionManager
import dropit.domain.service.TransferService
import dropit.infrastructure.event.EventBus
import javax.inject.Singleton

@Singleton
class StatusService(
    private val bus: EventBus,
    private val transferService: TransferService,
    private val phoneSessionManager: PhoneSessionManager
)

