package dropit.infrastructure.i18n

import java.io.InputStream
import java.text.MessageFormat
import java.util.*
import kotlin.collections.HashMap

class MessageSource(baseNames: Array<String>, locale: Locale, defaultLocale: Locale) {
    private val keys = baseNames
        .map { inputStreamFor(it, locale, defaultLocale) }
        .map { Properties().apply { load(it) }.entries }
        .let {
            val map = HashMap<String, String>()
            it.forEach { set ->
                set.forEach { entry ->
                    if (entry.key is String && entry.value is String) {
                        map[entry.key as String] = entry.value as String
                    }
                }
            }
            map
        }

    fun get(key: String, vararg args: Any): String {
        return if (keys.containsKey(key)) {
            MessageFormat(keys[key])
                .format(args)
        } else {
            "UNTRANSLATED: $key"
        }
    }

    private fun inputStreamFor(name: String, locale: Locale, defaultLocale: Locale): InputStream {
        return listOf(
            "${name}_${locale.language}_${locale.country}",
            "${name}_${locale.language}",
            "${name}_${defaultLocale.language}_${defaultLocale.country}",
            "${name}_${defaultLocale.language}"
        ).stream()
            .map { javaClass.getResourceAsStream("$it.properties") }
            .filter { it != null }
            .findFirst()
            .get()
    }
}

private val messageSource = MessageSource(
    arrayOf("/i18n/strings"),
    Locale.getDefault(),
    Locale.ENGLISH
)

fun t(str: String, vararg args: Any) = messageSource.get(str, *args)