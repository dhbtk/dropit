package dropit.mobile.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import dagger.android.support.DaggerAppCompatActivity
import dropit.application.discovery.DiscoveryClient
import dropit.infrastructure.event.EventBus
import dropit.mobile.R
import dropit.mobile.application.connection.ServerConnectionService
import dropit.mobile.databinding.ActivityMainBinding
import dropit.mobile.lib.db.SQLiteHelper
import dropit.mobile.lib.preferences.PreferencesHelper
import dropit.mobile.ui.configuration.ExternalStorageDialogFragment
import dropit.mobile.ui.configuration.REQUEST_WRITE_EXTERNAL_STORAGE
import java9.util.concurrent.CompletableFuture
import javax.inject.Inject

class MainActivity : DaggerAppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration

    @Inject
    lateinit var sqLiteHelper: SQLiteHelper

    @Inject
    lateinit var preferencesHelper: PreferencesHelper

    @Inject
    lateinit var eventBus: EventBus

    @Inject
    lateinit var discoveryClient: DiscoveryClient

    lateinit var subscription: EventBus.Subscription
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        if (preferencesHelper.currentComputerId != null) {
            ServerConnectionService.start(this)
        }
        subscription = eventBus.subscribe(DiscoveryClient.DiscoveryEvent::class, ::onBroadcast)
        discoveryClient.start()
        requestExternalStoragePermission()
    }

    override fun onPause() {
        super.onPause()
        eventBus.unsubscribe(subscription)
        discoveryClient.stop()
    }

    private fun onBroadcast(broadcast: DiscoveryClient.ServerBroadcast) {
        CompletableFuture.runAsync {
            sqLiteHelper.saveFromBroadcast(broadcast)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun requestExternalStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                ExternalStorageDialogFragment().show(supportFragmentManager, "t")
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_WRITE_EXTERNAL_STORAGE
                )
            }
        }
    }
}
