package com.bloki.adblocker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloki.adblocker.BlokiApp
import com.bloki.adblocker.vpn.AdBlockVpnService
import kotlinx.coroutines.launch

@Composable
fun MainScreen(onToggleVpn: (Boolean) -> Unit) {
    val app = BlokiApp.instance
    var vpnEnabled by remember { mutableStateOf(AdBlockVpnService.isRunning) }
    val scope = rememberCoroutineScope()

    val startOfDay = remember {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    }

    val blockedToday by app.database.statsDao().blockedCountSince(startOfDay)
        .collectAsState(initial = 0)
    val totalToday by app.database.statsDao().totalCountSince(startOfDay)
        .collectAsState(initial = 0)

    val blocklistReady by app.blocklistReady.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Bloki",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = when {
                !blocklistReady -> "Loading blocklists\u2026"
                vpnEnabled -> "Protection Active"
                else -> "Protection Disabled"
            },
            style = MaterialTheme.typography.titleMedium,
            color = when {
                !blocklistReady -> MaterialTheme.colorScheme.onSurfaceVariant
                vpnEnabled -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!blocklistReady) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        } else {
            Switch(
                checked = vpnEnabled,
                onCheckedChange = { enabled ->
                    vpnEnabled = enabled
                    onToggleVpn(enabled)
                },
                modifier = Modifier
                    .padding(8.dp)
                    .scale(3f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                label = "Blocked Today",
                value = blockedToday.toString(),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            StatCard(
                label = "Total Queries",
                value = totalToday.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        if (totalToday > 0) {
            val percentage = (blockedToday * 100) / totalToday
            StatCard(
                label = "Block Rate",
                value = "$percentage%",
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "${app.blocklistEngine.blockedCount} domains in blocklist",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
