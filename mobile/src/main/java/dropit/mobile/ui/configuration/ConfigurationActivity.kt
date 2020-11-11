package dropit.mobile.ui.configuration

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.discovery.DiscoveryClient
import dropit.application.dto.BroadcastMessage
import dropit.infrastructure.event.EventBus
import dropit.mobile.R
import dropit.mobile.domain.entity.Computer
import dropit.mobile.domain.service.ServerConnectionService
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import dropit.mobile.ui.configuration.adapter.ServerListAdapter
import java9.util.concurrent.CompletableFuture
import kotlinx.android.synthetic.main.activity_configuration.*
import java.net.InetAddress
import java.util.*

const val REQUEST_WRITE_EXTERNAL_STORAGE = 1

class ConfigurationActivity : AppCompatActivity() {
    val eventBus = EventBus()
    val objectMapper = ObjectMapper().apply { this.findAndRegisterModules() }
    val serverListAdapter = ServerListAdapter(this, ArrayList(), this::handleListClick)
    lateinit var discoveryClient: DiscoveryClient
    lateinit var sqliteHelper: SQLiteHelper
    lateinit var preferencesHelper: PreferencesHelper
    val seenComputerIds = HashSet<UUID>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sqliteHelper = SQLiteHelper(this)
        preferencesHelper = PreferencesHelper(this)
        eventBus.subscribe(DiscoveryClient.DiscoveryEvent::class) { (broadcast) ->
            handleBroadcast(broadcast)
        }
        TrafficStats.setThreadStatsTag(1)
        discoveryClient = DiscoveryClient(objectMapper, eventBus)

        setContentView(R.layout.activity_configuration)
        serverListView.layoutManager = LinearLayoutManager(this.applicationContext)
        serverListView.itemAnimator = DefaultItemAnimator()
        serverListView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        serverListView.adapter = serverListAdapter

        requestExternalStoragePermission()

        if (intent.dataString != null) pairFromIntentUrl()

        refreshServerList()
        ServerConnectionService.enqueueWork(this, Intent())
    }

    private fun pairFromIntentUrl() {
        val uri = Uri.parse(intent.dataString)
        if (uri.host != "pair") return

        val computerName = uri.getQueryParameter("computerName")
        val computerId = uri.getQueryParameter("computerId")?.let { UUID.fromString(it) }
        val serverPort = uri.getQueryParameter("serverPort")?.toInt()
        val ipAddress = uri.getQueryParameter("ipAddress")
        if (computerName == null || computerId == null || serverPort == null || ipAddress == null) return

        val broadcast = DiscoveryClient.ServerBroadcast(
                BroadcastMessage(computerName, computerId, serverPort),
                InetAddress.getByName(ipAddress)
        )
        seenComputerIds.add(computerId)
        val computer = sqliteHelper.saveFromBroadcast(broadcast)
        refreshServerList()
        PairingDialogFragment.create(computer).show(supportFragmentManager, "t")
    }

    fun requestExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ExternalStorageDialogFragment().show(fragmentManager, "t")
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getStringArrayList("seenIds")?.forEach { seenComputerIds.add(UUID.fromString(it)) }
        refreshServerList()
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState.putStringArrayList("seenIds", ArrayList(seenComputerIds.map(UUID::toString)))
    }

    override fun onPause() {
        super.onPause()
        discoveryClient.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        discoveryClient.stop()
    }

    private fun handleListClick(computer: Computer) {
        PairingDialogFragment.create(computer)
            .show(supportFragmentManager, "t")
    }

    /**
     * Runs in background
     */
    private fun handleBroadcast(broadcast: DiscoveryClient.ServerBroadcast) {
        CompletableFuture.supplyAsync {
            seenComputerIds.add(broadcast.data.computerId)
            sqliteHelper.saveFromBroadcast(broadcast)
        }.thenRun { refreshServerList() }
    }

    private fun refreshServerList() {
        CompletableFuture.supplyAsync {
            sqliteHelper.getComputers(seenComputerIds).map {
                it.copy(default = it.id == preferencesHelper.currentComputerId)
            }
        }.thenAccept { newList ->
            onMainThread {
                val oldList = serverListAdapter.items
                newList.forEach { newComputer ->
                    if (oldList.isEmpty()) {
                        oldList.add(newComputer)
                        serverListAdapter.notifyDataSetChanged()
                    } else if (!oldList.contains(newComputer)) {
                        val oldIndex = oldList.find {
                            it.id == newComputer.id
                        }.let { oldList.indexOf(it) }
                        if (oldIndex != -1) {
                            oldList[oldIndex] = newComputer
                            serverListAdapter.notifyItemChanged(oldIndex)
                        } else {
                            val firstIndex = oldList.find {
                                it.name > newComputer.name
                            }.let { oldList.indexOf(it) }
                            if (firstIndex == -1) {
                                oldList.add(newComputer)
                                serverListAdapter.notifyItemInserted(oldList.size - 1)
                            } else {
                                oldList.add(firstIndex, newComputer)
                                serverListAdapter.notifyItemInserted(firstIndex)
                            }
                        }
                    }
                }
            }
        }
    }
}
