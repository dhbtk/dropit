package dropit.mobile.lib.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import dropit.application.discovery.DiscoveryClient
import dropit.application.dto.TokenStatus
import dropit.mobile.application.entity.Computer
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import org.jetbrains.anko.db.insert
import org.jetbrains.anko.db.select
import org.jetbrains.anko.db.update
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

const val CURRENT_VERSION = 1
const val COMPUTER_TABLE = "computer"
const val ID_COLUMN = "id"
const val SECRET_COLUMN = "secret"
const val NAME_COLUMN = "name"
const val IP_ADDRESS_COLUMN = "ip_address"
const val PORT_COLUMN = "port"
const val TOKEN_COLUMN = "token"
const val TOKEN_STATUS_COLUMN = "token_status"

@Singleton
class SQLiteHelper @Inject constructor(val context: Context) :
    ManagedSQLiteOpenHelper(context, "DropIt", null, CURRENT_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        for (i in 1..CURRENT_VERSION) {
            readExec(db, "db_v$i.sql")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        for (i in (oldVersion + 1)..newVersion) {
            readExec(db, "db_v$i.sql")
        }
    }

    private fun readExec(db: SQLiteDatabase, fileName: String) {
        context.assets.open(fileName).use {
            InputStreamReader(it).use { isr ->
                BufferedReader(isr).use { reader ->
                    runScript(db, reader)
                }
            }
        }
    }

    private fun runScript(db: SQLiteDatabase?, reader: BufferedReader) {
        var line = reader.readLine()
        val statement = StringBuilder()
        while (line != null) {
            statement.append(line)
            statement.append("\n")
            if (line.endsWith(";")) {
                db?.execSQL(statement.toString())
                statement.clear()
            }
            line = reader.readLine()
        }
    }

    fun deleteComputer(computer: Computer) {
        use {
            delete(COMPUTER_TABLE, "$ID_COLUMN = ?", arrayOf(computer.id.toString()))
        }
    }

    fun getComputers(uuids: Set<UUID>): List<Computer> {
        return use {
            select(COMPUTER_TABLE)
                .columns(
                    ID_COLUMN,
                    SECRET_COLUMN,
                    NAME_COLUMN,
                    IP_ADDRESS_COLUMN,
                    PORT_COLUMN,
                    TOKEN_COLUMN,
                    TOKEN_STATUS_COLUMN
                )
                .whereSimple(
                    "$ID_COLUMN IN (${uuids.joinToString(", ") { "?" }})",
                    *uuids.map { it.toString() }.toTypedArray()
                )
                .orderBy(NAME_COLUMN)
                .exec {
                    val output = ArrayList<Computer>()
                    while (moveToNext()) {
                        output.add(cursorToComputer(this))
                    }
                    output
                }
        }
    }

    fun saveFromBroadcast(broadcast: DiscoveryClient.ServerBroadcast): Computer {
        return use {
            val exists = select(COMPUTER_TABLE)
                .column(ID_COLUMN)
                .whereSimple("$ID_COLUMN = ?", broadcast.data.computerId.toString())
                .exec { moveToFirst() }
            if (exists) {
                update(
                    COMPUTER_TABLE,
                    NAME_COLUMN to broadcast.data.computerName,
                    PORT_COLUMN to broadcast.data.port,
                    IP_ADDRESS_COLUMN to broadcast.ip.hostAddress
                )
                    .whereSimple("$ID_COLUMN = ?", broadcast.data.computerId.toString())
                    .exec()
            } else {
                insert(
                    COMPUTER_TABLE,
                    ID_COLUMN to broadcast.data.computerId.toString(),
                    NAME_COLUMN to broadcast.data.computerName,
                    PORT_COLUMN to broadcast.data.port,
                    IP_ADDRESS_COLUMN to broadcast.ip.hostAddress
                )
            }
            getComputer(broadcast.data.computerId)
        }
    }

    fun getComputer(id: UUID): Computer {
        return use {
            select(COMPUTER_TABLE)
                .columns(
                    ID_COLUMN,
                    SECRET_COLUMN,
                    NAME_COLUMN,
                    IP_ADDRESS_COLUMN,
                    PORT_COLUMN,
                    TOKEN_COLUMN,
                    TOKEN_STATUS_COLUMN
                )
                .whereSimple("$ID_COLUMN = ?", id.toString())
                .exec {
                    if (moveToFirst()) {
                        cursorToComputer(this)
                    } else {
                        throw RuntimeException("Computer not found: $id")
                    }
                }
        }
    }

    private fun cursorToComputer(cursor: Cursor): Computer {
        return Computer(
            UUID.fromString(cursor.getString(cursor.getColumnIndex(ID_COLUMN))),
            cursor.getString(cursor.getColumnIndex(SECRET_COLUMN))?.let { UUID.fromString(it) },
            cursor.getString(cursor.getColumnIndex(NAME_COLUMN)),
            cursor.getString(cursor.getColumnIndex(IP_ADDRESS_COLUMN)),
            cursor.getInt(cursor.getColumnIndex(PORT_COLUMN)),
            cursor.getString(cursor.getColumnIndex(TOKEN_COLUMN))?.let { UUID.fromString(it) },
            cursor.getInt(cursor.getColumnIndex(TOKEN_STATUS_COLUMN))
                .let { if (it == -1) null else TokenStatus.values()[it] }
        )
    }

    fun insertComputer(computer: Computer): Computer {
        return use {
            insert(
                COMPUTER_TABLE,
                ID_COLUMN to computer.id.toString(),
                NAME_COLUMN to computer.name,
                PORT_COLUMN to computer.port,
                IP_ADDRESS_COLUMN to computer.ipAddress,
                SECRET_COLUMN to computer.secret.toString(),
                NAME_COLUMN to computer.name,
                IP_ADDRESS_COLUMN to computer.ipAddress,
                PORT_COLUMN to computer.port,
                TOKEN_COLUMN to computer.token?.toString(),
                TOKEN_STATUS_COLUMN to (computer.tokenStatus?.ordinal ?: -1)
            )
            getComputer(computer.id)
        }
    }

    fun updateComputer(computer: Computer): Computer {
        return use {
            update(
                COMPUTER_TABLE,
                SECRET_COLUMN to computer.secret.toString(),
                NAME_COLUMN to computer.name,
                IP_ADDRESS_COLUMN to computer.ipAddress,
                PORT_COLUMN to computer.port,
                TOKEN_COLUMN to computer.token?.toString(),
                TOKEN_STATUS_COLUMN to (computer.tokenStatus?.ordinal ?: -1)
            )
                .whereSimple("$ID_COLUMN = ?", computer.id.toString())
                .exec()
            getComputer(computer.id)
        }
    }
}