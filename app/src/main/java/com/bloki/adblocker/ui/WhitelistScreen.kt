package com.bloki.adblocker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bloki.adblocker.BlokiApp
import com.bloki.adblocker.data.WhitelistEntry
import kotlinx.coroutines.launch

@Composable
fun WhitelistScreen() {
    val app = BlokiApp.instance
    val dao = app.database.whitelistDao()
    val entries by dao.getAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var newDomain by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Whitelist",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = newDomain,
                onValueChange = { newDomain = it },
                label = { Text("Domain") },
                placeholder = { Text("example.com") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(
                onClick = {
                    val domain = newDomain.trim().lowercase()
                    if (domain.isNotEmpty()) {
                        scope.launch {
                            dao.insert(WhitelistEntry(domain = domain))
                            app.blocklistEngine.addWhitelistDomain(domain)
                        }
                        newDomain = ""
                    }
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(entries, key = { it.domain }) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.domain,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = {
                            scope.launch {
                                dao.delete(entry)
                                app.blocklistEngine.removeWhitelistDomain(entry.domain)
                            }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
