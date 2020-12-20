package dropit.infrastructure.db

import dropit.logger
import org.jooq.DSLContext
import org.jooq.ExecuteContext
import org.jooq.SQLDialect
import org.jooq.conf.RenderFormatting
import org.jooq.conf.Settings
import org.jooq.impl.DSL
import org.jooq.impl.DefaultExecuteListener
import java.util.concurrent.ConcurrentHashMap

class SqlLogger : DefaultExecuteListener() {
    private val dslContexts = ConcurrentHashMap<SQLDialect, DSLContext>()

    override fun executeStart(ctx: ExecuteContext) {
        if (ctx.query() != null) {
            val stack = Thread.currentThread().stackTrace
            val lastIndex = stack.indexOfLast { it.className.startsWith("org.jooq.impl") } + 1
            val element = stack[lastIndex]
            logger.debug(
                "${element.fileName}:${element.lineNumber} ${
                    jooq(ctx.dialect()).renderInlined(
                        ctx.query()
                    )
                }"
            )
        }
    }

    private fun jooq(dialect: SQLDialect): DSLContext {
        return dslContexts.computeIfAbsent(dialect) {
            DSL.using(
                dialect, Settings().withRenderFormatted(true).withRenderFormatting(
                    RenderFormatting().withIndentation("").withNewline(" ")
                )
            )
        }
    }
}