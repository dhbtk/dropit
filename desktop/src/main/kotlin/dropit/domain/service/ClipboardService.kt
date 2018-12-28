package dropit.domain.service

import dropit.infrastructure.event.AppEvent
import dropit.infrastructure.event.EventBus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardService @Inject constructor(private val bus: EventBus) {
    data class ClipboardReceiveEvent(override val payload: String) : AppEvent<String>

    fun receive(data: String) {
        bus.broadcast(ClipboardReceiveEvent(data))
    }
}