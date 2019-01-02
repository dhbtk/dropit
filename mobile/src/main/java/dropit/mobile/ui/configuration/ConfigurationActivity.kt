package dropit.mobile.ui.configuration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import com.fasterxml.jackson.databind.ObjectMapper
import dropit.application.discovery.DiscoveryClient
import dropit.infrastructure.event.EventBus
import dropit.mobile.R
import dropit.mobile.domain.entity.Computer
import dropit.mobile.infrastructure.db.SQLiteHelper
import dropit.mobile.infrastructure.preferences.PreferencesHelper
import dropit.mobile.onMainThread
import dropit.mobile.ui.configuration.adapter.ServerListAdapter
import java9.util.concurrent.CompletableFuture
import kotlinx.android.synthetic.main.activity_configuration.*
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
        discoveryClient = DiscoveryClient(objectMapper, eventBus)

        setContentView(R.layout.activity_configuration)
        serverListView.layoutManager = LinearLayoutManager(this.applicationContext)
        serverListView.itemAnimator = DefaultItemAnimator()
        serverListView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        serverListView.adapter = serverListAdapter

        requestExternalStoragePermission()
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

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState?.getStringArrayList("seenIds")?.forEach { seenComputerIds.add(UUID.fromString(it)) }
        refreshServerList()
    }

    override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
        super.onSaveInstanceState(outState, outPersistentState)
        outState?.putStringArrayList("seenIds", ArrayList(seenComputerIds.map(UUID::toString)))
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