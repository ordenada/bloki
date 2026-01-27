package com.bloki.adblocker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bloki.adblocker.BlokiApp
import com.bloki.adblocker.data.BlocklistSource
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BlocklistScreen() {
    val app = BlokiApp.instance
    val dao = app.database.blocklistSourceDao()
    val sources by dao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var updating by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Blocklists",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                updating = true
                scope.launch {
                    app.blocklistManager.updateLists()
                    updating = false
                }
            },
            enabled = !updating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (updating) "Updating..." else "Update All Lists")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sources, key = { it.url }) { source ->
                BlocklistCard(
                    source = source,
                    onToggle = { enabled ->
                        scope.launch { dao.update(source.copy(enabled = enabled)) }
                    },
                    onDelete = {
                        scope.launch { dao.delete(source.url) }
                    }
                )
            }
        }
    }
}

@Composable
fun BlocklistCard(
    source: BlocklistSource,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${source.domainCount} domains",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (source.lastUpdated > 0) {
                    Text(
                        text = "Updated: ${dateFormat.format(Date(source.lastUpdated))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Switch(
                checked = source.enabled,
                onCheckedChange = onToggle
            )
        }
    }
}
