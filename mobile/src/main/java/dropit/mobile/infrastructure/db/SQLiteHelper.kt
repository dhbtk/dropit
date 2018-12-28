package dropit.mobile.infrastructure.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import dropit.application.discovery.DiscoveryClient
import dropit.application.dto.TokenStatus
import dropit.mobile.domain.entity.Computer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

const val CURRENT_VERSION = 1
const val COMPUTER_TABLE = "computer"
const val ID_COLUMN = "id"
const val SECRET_COLUMN = "secret"
const val NAME_COLUMN = "name"
const val IP_ADDRESS_COLUMN = "ip_address"
const val PORT_COLUMN = "port"
const val TOKEN_COLUMN = "token"
const val TOKEN_STATUS_COLUMN = "token_status"

class SQLiteHelper(val context: Context) : SQLiteOpenHelper(context, "DropIt", null, CURRENT_VERSION) {

    override fun onCreate(db: SQLiteDatabase?) {
        for (i in 1..CURRENT_VERSION) {
            readExec(db, "db_v$i.sql")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        for (i in (oldVersion + 1)..newVersion) {
            readExec(db, "db_v$i.sql")
        }
    }

    private fun readExec(db: SQLiteDatabase?, fileName: String) {
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

    fun getComputers(uuids: Set<UUID>): List<Computer> {
        return readableDatabase.use {
            it.query(
                COMPUTER_TABLE,
                arrayOf(ID_COLUMN, SECRET_COLUMN, NAME_COLUMN, IP_ADDRESS_COLUMN, PORT_COLUMN, TOKEN_COLUMN, TOKEN_STATUS_COLUMN),
                "$ID_COLUMN IN (${uuids.joinToString(", ") { "?" }})", uuids.map { it.toString() }.toTypedArray(),
                null, null,
                NAME_COLUMN
            ).use { cursor ->
                val output = ArrayList<Computer>()
                while (cursor.moveToNext()) {
                    output.add(cursorToComputer(cursor))
                }
                output
            }
        }
    }

    fun saveFromBroadcast(broadcast: DiscoveryClient.ServerBroadcast): Computer {
        val exists = readableDatabase.use {
            it.query(
                COMPUTER_TABLE,
                arrayOf(ID_COLUMN),
                "$ID_COLUMN = ?", arrayOf(broadcast.data.computerId.toString()),
                null, null, null
            ).use { it.moveToFirst() }
        }
        writableDatabase.use {
            if (exists) {
                it.update(
                    COMPUTER_TABLE,
                    ContentValues().apply {
                        put(NAME_COLUMN, broadcast.data.computerName)
                        put(PORT_COLUMN, broadcast.data.port)
                        put(IP_ADDRESS_COLUMN, broadcast.ip.hostAddress)
                    },
                    "$ID_COLUMN = ?", arrayOf(broadcast.data.computerId.toString())
                ).toLong()
            } else {
                it.insert(
                    COMPUTER_TABLE,
                    null,
                    ContentValues().apply {
                        put(ID_COLUMN, broadcast.data.computerId.toString())
                        put(NAME_COLUMN, broadcast.data.computerName)
                        put(PORT_COLUMN, broadcast.data.port)
                        put(IP_ADDRESS_COLUMN, broadcast.ip.hostAddress)
                    }
                )
            }
        }
        return getComputer(broadcast.data.computerId)
    }

    fun getComputer(id: UUID): Computer {
        return readableDatabase.use {
            it.query(
                COMPUTER_TABLE,
                arrayOf(ID_COLUMN, SECRET_COLUMN, NAME_COLUMN, IP_ADDRESS_COLUMN, PORT_COLUMN, TOKEN_COLUMN, TOKEN_STATUS_COLUMN),
                "$ID_COLUMN = ?", arrayOf(id.toString()),
                null, null,
                NAME_COLUMN
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    cursorToComputer(cursor)
                } else {
                    throw RuntimeException()
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
            cursor.getInt(cursor.getColumnIndex(TOKEN_STATUS_COLUMN)).let { if (it == -1) null else TokenStatus.values()[it] }
        )
    }

    fun updateComputer(computer: Computer): Computer {
        writableDatabase.use {
            it.update(
                COMPUTER_TABLE,
                ContentValues().apply {
                    put(SECRET_COLUMN, computer.secret.toString())
                    put(NAME_COLUMN, computer.name)
                    put(IP_ADDRESS_COLUMN, computer.ipAddress)
                    put(PORT_COLUMN, computer.port)
                    put(TOKEN_COLUMN, computer.token?.toString())
                    put(TOKEN_STATUS_COLUMN, computer.tokenStatus?.ordinal ?: -1)
                },
                "$ID_COLUMN = ?", arrayOf(computer.id.toString())
            )
        }
        return getComputer(computer.id)
    }
}