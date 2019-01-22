package dropit.infrastructure.db

import org.jooq.RecordContext
import org.jooq.impl.DefaultRecordListener
import java.lang.reflect.Method
import java.time.LocalDateTime

class CrudListener : DefaultRecordListener() {
    val methodCache = HashMap<Class<*>, Method>()
    val nxCache = HashSet<Class<*>>()
    override fun updateStart(ctx: RecordContext) {
        val clazz = ctx.record().javaClass
        if (!nxCache.contains(clazz)) {
            if (methodCache.containsKey(clazz)) {
                methodCache[clazz]?.invoke(ctx.record(), LocalDateTime.now())
            } else {
                try {
                    methodCache[clazz] = clazz.getMethod("setUpdatedAt", LocalDateTime::class.java)
                    methodCache[clazz]?.invoke(ctx.record(), LocalDateTime.now())
                } catch (e: NoSuchMethodException) {
                    nxCache.add(clazz)
                }
            }
        }
    }
}