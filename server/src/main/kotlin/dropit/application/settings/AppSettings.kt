package dropit.application.settings

import dropit.domain.entity.ShowFileAction
import dropit.infrastructure.i18n.t
import dropit.jooq.tables.pojos.Settings
import dropit.jooq.tables.records.SettingsRecord
import dropit.jooq.tables.references.SETTINGS
import org.jooq.DSLContext
import java.net.InetAddress
import java.net.UnknownHostException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty
import kotlin.reflect.full.memberProperties

class AppSettings(val jooq: DSLContext) {
    private val record = createDefaultSettings()
    var firstStart: Boolean = false
    var computerId: UUID by Delegate(record)
    var computerSecret: UUID by Delegate(record)
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
        private val propMap = HashMap<KProperty<*>, KMutableProperty1<SettingsRecord, Any?>>()
        
        operator fun setValue(thisRef: AppSettings, property: KProperty<*>, value: T) {
            getProperty(record, property).also { prop ->
                prop.set(record, value)
                jooq.update(SETTINGS).set(record).execute()
            }
        }

        operator fun getValue(thisRef: AppSettings, property: KProperty<*>): T {
            return getProperty(record, property).get(record) as T
        }
        
        private fun getProperty(thisRef: SettingsRecord, property: KProperty<*>): KMutableProperty1<SettingsRecord, Any?> {
            return propMap.computeIfAbsent(property) {
                    p ->
                (thisRef::class.memberProperties.find {
                    it is KMutableProperty1 && it.name == p.name
                } as KMutableProperty1<SettingsRecord, Any?>?)!!
            }
        }
    }

    fun createDefaultSettings(): SettingsRecord {
        if (jooq.fetchOne(SETTINGS) == null) {
            val settings = Settings(
                computerName = getDefaultComputerName(),
                rootTransferFolder = getDefaultTransferFolder(),
                transferFolderName = "{0,date,yyyy-MM-dd HH-mm} {1}",
                computerId = UUID.randomUUID(),
                computerSecret = UUID.randomUUID().toString(),
                serverPort = 58992,
                separateTransferFolders = true,
                openTransferOnCompletion = true,
                showTransferAction = ShowFileAction.OPEN_FILE,
                logClipboardTransfers = true,
                keepWindowOnTop = false
            )
            jooq.newRecord(SETTINGS, settings).insert()
            firstStart = true
        }
        return jooq.fetchOne(SETTINGS)!!
    }

    private fun getDefaultTransferFolder(): String {
        return if (System.getProperty("dropit.test") == "true") {
            val folder = Files.createTempDirectory("dropit")
            folder.toString()
        } else {
            val path = Paths.get(System.getProperty("user.home"), t("appSettings.init.defaultTransferFolder"))
            if (!(path.toFile().exists() && path.toFile().isDirectory)) {
                if (!path.toFile().mkdirs()) {
                    throw IllegalStateException("Could not create default transfer folder $path")
                }
            }
            return path.toString()
        }
    }

    private fun getDefaultComputerName(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: UnknownHostException) {
            t("appSettings.init.defaultComputerName", System.getProperty("user.name"))
        }
    }
}
