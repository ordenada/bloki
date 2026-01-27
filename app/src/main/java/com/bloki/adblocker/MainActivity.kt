package com.bloki.adblocker

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bloki.adblocker.ui.BlocklistScreen
import com.bloki.adblocker.ui.MainScreen
import com.bloki.adblocker.ui.StatsScreen
import com.bloki.adblocker.ui.WhitelistScreen
import com.bloki.adblocker.ui.theme.BlokiTheme
import com.bloki.adblocker.vpn.AdBlockVpnService

class MainActivity : ComponentActivity() {

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startVpn()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BlokiTheme {
                AppNavigation(
                    onToggleVpn = { enabled ->
                        if (enabled) requestVpnPermission() else stopVpn()
                    }
                )
            }
        }
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpn()
        }
    }

    private fun startVpn() {
        val intent = Intent(this, AdBlockVpnService::class.java)
            .setAction(AdBlockVpnService.ACTION_START)
        startService(intent)
    }

    private fun stopVpn() {
        val intent = Intent(this, AdBlockVpnService::class.java)
            .setAction(AdBlockVpnService.ACTION_STOP)
        startService(intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(onToggleVpn: (Boolean) -> Unit) {
    val navController = rememberNavController()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabs = listOf("Dashboard", "Blocklists", "Whitelist", "Stats")
    val icons = listOf(Icons.Default.Home, Icons.Default.List, Icons.Default.Check, Icons.Default.Info)

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            navController.navigate(title) {
                                popUpTo("Dashboard") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "Dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("Dashboard") { MainScreen(onToggleVpn = onToggleVpn) }
            composable("Blocklists") { BlocklistScreen() }
            composable("Whitelist") { WhitelistScreen() }
            composable("Stats") { StatsScreen() }
        }
    }
}
