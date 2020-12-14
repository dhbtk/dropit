package dropit.application.settings

import dropit.application.model.DefaultSettings.defaultSettings
import dropit.application.model.ShowFileAction
import dropit.jooq.tables.records.SettingsRecord
import dropit.jooq.tables.references.SETTINGS
import org.jooq.DSLContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

@Singleton
class AppSettings @Inject constructor(val jooq: DSLContext) {
    val firstStart = jooq.fetchOne(SETTINGS) == null
    private val record = createDefaultSettings()
    var computerId: UUID by Delegate(record)
    var computerSecret: String by Delegate(record)
    var computerName: String by Delegate(record)
    var transferFolderName: String by Delegate(record)
    var rootTransferFolder: String by Delegate(record)
    var serverPort: Int by Delegate(record)
    var currentPhoneId: UUID? by Delegate(record)
    var separateTransferFolders: Boolean by Delegate(record)
    var openTransferOnCompletion: Boolean by Delegate(record)
    var showTransferAction: ShowFileAction by Delegate(record)
    var logClipboardTransfers: Boolean by Delegate(record)
    var keepWindowOnTop: Boolean by Delegate(record)

    private inner class Delegate<T>(private val record: SettingsRecord) {
        private val propMap = HashMap<KProperty<*>, KMutableProperty1<SettingsRecord, T>>()
        
        operator fun setValue(thisRef: AppSettings, property: KProperty<*>, value: T) {
            getProperty(record, property).also { prop ->
                prop.set(record, value)
                jooq.update(SETTINGS).set(record).execute()
            }
        }

        operator fun getValue(thisRef: AppSettings, property: KProperty<*>): T {
            return getProperty(record, property).get(record)
        }

        @Suppress("UNCHECKED_CAST")
        private fun getProperty(thisRef: SettingsRecord, property: KProperty<*>): KMutableProperty1<SettingsRecord, T> {
            return propMap.computeIfAbsent(property) { p ->
                thisRef::class.memberProperties
                    .find { it is KMutableProperty1 && it.name == p.name }
                    .let { it as KMutableProperty1<SettingsRecord, T> }
            }
        }
    }

    fun createDefaultSettings(): SettingsRecord {
        if (jooq.fetchOne(SETTINGS) == null) {
            jooq.newRecord(SETTINGS, defaultSettings).insert()
        }
        return jooq.fetchOne(SETTINGS)!!
    }
}
