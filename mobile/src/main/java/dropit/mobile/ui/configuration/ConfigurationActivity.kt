package dropit.mobile.ui.configuration

import android.os.Bundle
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
import dropit.mobile.ui.sending.PairingDialogFragment
import dropit.mobile.ui.sending.adapter.ServerListAdapter
import kotlinx.android.synthetic.main.activity_configuration.*
import java.util.*

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
        seenComputerIds.add(broadcast.data.computerId)
        sqliteHelper.saveFromBroadcast(broadcast)
        val newList = sqliteHelper.getComputers(seenComputerIds).map {
            it.copy(default = it.id.toString() == preferencesHelper.currentComputerId)
        }

        onMainThread {
            serverListAdapter.items.clear()
            serverListAdapter.items.addAll(newList)
            serverListAdapter.notifyDataSetChanged()
        }
    }
}