package dropit.application.model

import org.jooq.Record
import java.util.*
import kotlin.reflect.KProperty

class ExtLazy<T, R : Record>(val refresh: R.() -> T) {
    private val refMap = WeakHashMap<R, T>()

    operator fun getValue(record: R, property: KProperty<*>): T {
        return refMap.computeIfAbsent(record) {
            refresh(record)
        }
    }
}
