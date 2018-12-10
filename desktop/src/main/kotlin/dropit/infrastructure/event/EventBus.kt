package dropit.infrastructure.event

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.reflect.KClass

class EventBus {
    private val subscriptions = ConcurrentHashMap<KClass<out AppEvent<*>>, LinkedHashSet<EventHandler<AppEvent<*>>>>()
    private val log = Logger.getLogger(this::class.java.name)

    @Suppress("UNCHECKED_CAST")
    fun <T : AppEvent<*>> subscribe(type: KClass<T>, handler: EventHandler<T>): EventHandler<T> {
        subscriptions.compute(type) { _, set ->
            if(set == null) {
                LinkedHashSet(listOf(handler as EventHandler<AppEvent<*>>))
            } else {
                set += handler as EventHandler<AppEvent<*>>
                set
            }
        }
        return handler
    }

    fun <T : AppEvent<*>> unsubscribe(type: KClass<T>, handler: EventHandler<T>) {
        subscriptions.computeIfPresent(type) { _, set ->
            set.remove(handler)
            set
        }
    }

    fun broadcast(event: AppEvent<*>) {
        log.info("Broadcasting ${event::class.simpleName} - ${event}")
        subscriptions[event::class]?.forEach { it(event) }
    }
}

interface AppEvent<T> {
    val payload: T?
}

typealias EventHandler<T> = (T) -> Unit