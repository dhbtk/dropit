package dropit.infrastructure.i18n

import org.yaml.snakeyaml.Yaml
import java.io.InputStream
import java.text.MessageFormat
import java.util.*

class MessageSource(baseName: String, locale: Locale, defaultLocale: Locale) {
    private val keys = inputStreamFor(baseName, locale, defaultLocale)
        .let { Yaml().load<HashMap<String, Any>>(it) }

    fun get(key: String, vararg args: Any?): String {
        val template = dig(key)
        return if (template != null) {
            MessageFormat(template).format(args)
        } else {
            "UNTRANSLATED: $key"
        }
    }

    private fun dig(key: String): String? {
        var currentMap: HashMap<*, *> = keys
        val fullPath = key.split(".")
        for (s in fullPath) {
            val data = currentMap[s]
            if (data != null && data is HashMap<*, *>) {
                currentMap = data
            } else {
                break
            }
        }
        val str = currentMap[fullPath.last()]
        return if (str != null && str is String) {
            str
        } else {
            null
        }
    }

    private fun inputStreamFor(name: String, locale: Locale, defaultLocale: Locale): InputStream {
        return listOf(
            "${name}.${locale.language}-${locale.country}",
            "${name}.${locale.language}",
            "${name}.${defaultLocale.language}-${defaultLocale.country}",
            "${name}.${defaultLocale.language}"
        ).stream()
            .map { javaClass.getResourceAsStream("$it.yaml") }
            .filter { it != null }
            .findFirst()
            .get()
    }
}

private val messageSource = MessageSource(
    "/i18n/strings",
    Locale.getDefault(),
    Locale.ENGLISH
)

@Suppress("SpreadOperator")
fun t(str: String, vararg args: Any?) = messageSource.get(str, *args)
fun t(str: String) = messageSource.get(str)
