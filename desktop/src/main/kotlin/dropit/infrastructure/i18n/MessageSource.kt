package dropit.infrastructure.i18n

import org.springframework.context.support.ReloadableResourceBundleMessageSource
import java.util.*

object MessageSourceHolder {
    val messageSource = createMessageSource()

    private fun createMessageSource(): ReloadableResourceBundleMessageSource {
        val messageSource = ReloadableResourceBundleMessageSource()
        messageSource.setAlwaysUseMessageFormat(true)
        messageSource.setDefaultEncoding("UTF-8")
        messageSource.setBasenames("classpath:i18n/labels", "classpath:i18n/messages")
        return messageSource
    }
}

fun t(str: String, vararg args: Any): String = MessageSourceHolder.messageSource.getMessage(str, args, Locale.getDefault())