package dropit.infrastructure.event

import java.util.*
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.LinkedHashSet
import kotlin.reflect.KClass

@Singleton
class EventBus @Inject constructor() {
    private val subscriptions = HashMap<KClass<out AppEvent<*>>, LinkedHashSet<EventHandler<*>>>()
    private val log = Logger.getLogger(this::class.java.name)

    @Suppress("UNCHECKED_CAST")
    fun <T> subscribe(type: KClass<out AppEvent<T>>, handler: EventHandler<T>): Subscription {
        synchronized(subscriptions) {
            val set = subscriptions[type] ?: LinkedHashSet<EventHandler<*>>().also {
                subscriptions[type] = it
            }
            set.add(handler as EventHandler<*>)
        }
        return Subscription(handler)
    }

    fun subscribe(vararg types: KClass<out AppEvent<*>>, handler: () -> Unit): Subscription {
        val wrapped = EventHandler<Any?> { handler() }
        types.forEach { type ->
            synchronized(subscriptions) {
                val set = subscriptions[type] ?: LinkedHashSet<EventHandler<*>>().also {
                    subscriptions[type] = it
                }
                set.add(wrapped)
            }
        }
        return Subscription(wrapped)
    }

    fun unsubscribe(subscription: Subscription) {
        subscriptions.values.find { set ->
            set.remove(subscription.handler)
        }
    }

    fun broadcast(event: AppEvent<*>) {
        log.fine("Broadcasting ${event::class.simpleName} - $event")
        subscriptions[event::class]?.forEach {
            (it as EventHandler<Any?>).handle(event.payload)
        }
    }

    data class Subscription(val handler: EventHandler<*>)
}

interface AppEvent<T> {
    val payload: T?
}

fun interface EventHandler<T> {
    fun handle(payload: T)
}
