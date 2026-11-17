package ee.episodeday.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.work.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifications.createChannel(this)
        if (Build.VERSION.SDK_INT >= 33) requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        scheduleRefresh()
        setContent { EpisodeDayScreen() }
    }
    private fun scheduleRefresh() {
        val request = PeriodicWorkRequestBuilder<EpisodeWorker>(24, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("episode_refresh", ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    @Composable
    private fun EpisodeDayScreen() {
        var shows by remember { mutableStateOf(Storage.getShows(this)) }
        var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<Show>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf("Lisa seriaal, et näha uusi osi.") }
        var tab by remember { mutableIntStateOf(0) }

        fun refresh() {
            loading = true
            executor.execute {
                val fetched = TvMazeApi.refreshAll(shows)
                runOnUiThread {
                    episodes = fetched
                    Notifications.notifyToday(this, fetched)
                    loading = false
                    message = if (fetched.isEmpty()) "Kuupäevi ei leitud. Proovi hiljem uuesti." else "Andmed uuendatud."
                }
            }
        }
        LaunchedEffect(shows) { refresh() }

        MaterialTheme(colorScheme = darkColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFFB9A0FF),
            background = androidx.compose.ui.graphics.Color(0xFF10111A),
            surface = androidx.compose.ui.graphics.Color(0xFF1B1D2A)
        )) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Column(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("EpisodeDay", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Text("Sinu seriaalid ja uued osad ühes kohas", color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f))
                    }
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("▦") }, label = { Text("Tulemas") })
                        NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("＋") }, label = { Text("Lisa seriaal") })
                        NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("▣") }, label = { Text("Minu seriaalid") })
                    }
                }
            ) { padding ->
                Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 18.dp)) {
                    when (tab) {
                        0 -> {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Järgmised osad", style = MaterialTheme.typography.titleLarge)
                                TextButton(onClick = { refresh() }) { Text("Uuenda") }
                            }
                            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(message, color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f), style = MaterialTheme.typography.bodySmall)
                            val upcoming = episodes.filter { runCatching { LocalDate.parse(it.airdate).isAfter(LocalDate.now().minusDays(1)) }.getOrDefault(false) }
                            if (upcoming.isEmpty() && !loading) Text("Tulevasi osi pole veel teada.", modifier = Modifier.padding(top = 18.dp))
                            LazyColumn(contentPadding = PaddingValues(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                items(upcoming.take(100)) { ep -> EpisodeCard(ep) }
                            }
                        }
                        1 -> {
                            Text("Leia seriaal", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = query, onValueChange = { query = it },
                                label = { Text("Seriaali nimi") }, modifier = Modifier.fillMaxWidth(),
                                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    loading = true
                                    executor.execute {
                                        val found = runCatching { TvMazeApi.search(query) }.getOrDefault(emptyList())
                                        runOnUiThread { results = found; loading = false }
                                    }
                                })
                            )
                            Button(onClick = {
                                loading = true
                                executor.execute {
                                    val found = runCatching { TvMazeApi.search(query) }.getOrDefault(emptyList())
                                    runOnUiThread { results = found; loading = false }
                                }
                            }, modifier = Modifier.padding(top = 8.dp)) { Text("Otsi") }
                            if (loading) CircularProgressIndicator(Modifier.padding(12.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(results) { show ->
                                    Card {
                                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(show.name, Modifier.weight(1f))
                                            TextButton(onClick = {
                                                shows = (shows + show).distinctBy { it.id }
                                                Storage.saveShows(this@MainActivity, shows)
                                                results = results.filterNot { it.id == show.id }
                                                tab = 2
                                            }) { Text(if (shows.any { it.id == show.id }) "Lisatud" else "Lisa") }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            Text("Minu seriaalid", style = MaterialTheme.typography.titleLarge)
                            if (shows.isEmpty()) Text("Sa pole veel ühtegi seriaali lisanud.", Modifier.padding(top = 12.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(shows) { show ->
                                    Card {
                                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(show.name, Modifier.weight(1f))
                                            TextButton(onClick = {
                                                shows = shows.filterNot { it.id == show.id }
                                                Storage.saveShows(this@MainActivity, shows)
                                            }) { Text("Eemalda") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun EpisodeCard(ep: Episode) {
        val date = runCatching {
            LocalDate.parse(ep.airdate).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        }.getOrDefault(ep.airdate)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text(ep.showName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                Text(ep.name, style = MaterialTheme.typography.titleMedium)
                Text("$date  •  ${ep.season}. hooaeg, ${ep.number}. osa", color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
            }
        }
    }
}
