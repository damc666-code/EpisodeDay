package ee.episodeday.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import androidx.work.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val PageBg = Color(0xFF0B0D16)
private val CardBg = Color(0xFF171B2A)
private val Accent = Color(0xFFAA8CFF)
private val SoftText = Color(0xFFA7ADC2)

class MainActivity : ComponentActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    private val requestPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Notifications.createChannel(this)

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }

        scheduleRefresh()

        setContent {
            EpisodeDayScreen()
        }
    }

    private fun scheduleRefresh() {
        val request =
            PeriodicWorkRequestBuilder<EpisodeWorker>(
                24, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "episode_refresh",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    @Composable
    private fun EpisodeDayScreen() {
        var shows by remember {
            mutableStateOf(Storage.getShows(this))
        }

        var episodes by remember {
            mutableStateOf<List<Episode>>(emptyList())
        }

        var query by remember { mutableStateOf("") }
        var results by remember { mutableStateOf<List<Show>>(emptyList()) }
        var loading by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf("Laen sinu seriaale...") }
        var tab by remember { mutableIntStateOf(0) }

        fun refresh() {
            loading = true

            executor.execute {
                val fetched = TvMazeApi.refreshAll(shows)

                runOnUiThread {
                    episodes = fetched
                    Notifications.notifyToday(this, fetched)
                    loading = false
                    message = if (fetched.isEmpty()) {
                        "Lisa mõni seriaal, et näha uusi osi."
                    } else {
                        "Andmed uuendatud"
                    }
                }
            }
        }

        fun searchShows() {
            if (query.isBlank()) return

            loading = true

            executor.execute {
                val found = runCatching {
                    TvMazeApi.search(query)
                }.getOrDefault(emptyList())

                runOnUiThread {
                    results = found
                    loading = false
                }
            }
        }

        LaunchedEffect(shows) {
            refresh()
        }

        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Accent,
                background = PageBg,
                surface = CardBg,
                onBackground = Color.White,
                onSurface = Color.White
            )
        ) {
            Scaffold(
                containerColor = PageBg,
                bottomBar = {
                    NavigationBar(
                        containerColor = CardBg
                    ) {
                        NavigationBarItem(
                            selected = tab == 0,
                            onClick = { tab = 0 },
                            icon = { Text("⌂", fontSize = 22.sp) },
                            label = { Text("Kodu") }
                        )

                        NavigationBarItem(
                            selected = tab == 1,
                            onClick = { tab = 1 },
                            icon = { Text("＋", fontSize = 22.sp) },
                            label = { Text("Lisa") }
                        )

                        NavigationBarItem(
                            selected = tab == 2,
                            onClick = { tab = 2 },
                            icon = { Text("▣", fontSize = 22.sp) },
                            label = { Text("Minu sarjad") }
                        )
                    }
                }
            ) { padding ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "EPISODEDAY",
                                color = Accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 3.sp
                            )

                            Text(
                                when (tab) {
                                    0 -> "Sinu seriaalimaailm"
                                    1 -> "Leia uusi seriaale"
                                    else -> "Sinu kollektsioon"
                                },
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            "ED",
                            color = Accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBg)
                                .padding(14.dp)
                        )
                    }

                    when (tab) {
                        0 -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    start = 18.dp,
                                    end = 18.dp,
                                    bottom = 20.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {

                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = CardBg
                                        ),
                                        shape = RoundedCornerShape(22.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp)
                                        ) {
                                            Text(
                                                "TERE TULEMAST TAGASI",
                                                color = Accent,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                "Mis järgmisena vaatamiseks tuleb?",
                                                color = Color.White,
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                "${shows.size} seriaali  •  ${episodes.size} episoodi",
                                                color = SoftText
                                            )

                                            Spacer(Modifier.height(12.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = { refresh() },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Accent,
                                                        contentColor = PageBg
                                                    )
                                                ) {
                                                    Text("Uuenda andmeid")
                                                }

                                                Spacer(Modifier.width(10.dp))

                                                if (loading) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(22.dp),
                                                        color = Accent,
                                                        strokeWidth = 2.dp
                                                    )
                                                }
                                            }

                                            Text(
                                                message,
                                                color = SoftText,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                if (shows.isNotEmpty()) {
                                    item {
                                        SectionTitle("Minu seriaalid")
                                    }

                                    item {
                                        LazyRow(
                                            horizontalArrangement =
                                                Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(shows) { show ->
                                                ShowPosterCard(show)
                                            }
                                        }
                                    }
                                }

                                item {
                                    SectionTitle("Järgmised episoodid")
                                }

                                val upcoming = episodes.filter {
                                    runCatching {
                                        LocalDate.parse(it.airdate)
                                            .isAfter(LocalDate.now().minusDays(1))
                                    }.getOrDefault(false)
                                }.take(30)

                                if (upcoming.isEmpty()) {
                                    item {
                                        EmptyCard(
                                            "Tulevasi episoode pole veel teada."
                                        )
                                    }
                                } else {
                                    items(upcoming) { episode ->
                                        EpisodeCard(episode)
                                    }
                                }
                            }
                        }

                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp)
                            ) {
                                OutlinedTextField(
                                    value = query,
                                    onValueChange = { query = it },
                                    label = { Text("Seriaali nimi") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        imeAction = ImeAction.Search
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onSearch = { searchShows() }
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Accent,
                                        focusedLabelColor = Accent,
                                        cursorColor = Accent
                                    )
                                )

                                Spacer(Modifier.height(10.dp))

                                Button(
                                    onClick = { searchShows() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Accent,
                                        contentColor = PageBg
                                    )
                                ) {
                                    Text("OTSI SERIAALE")
                                }

                                if (loading) {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Accent
                                    )
                                }

                                LazyColumn(
                                    verticalArrangement =
                                        Arrangement.spacedBy(12.dp),
                                    contentPadding =
                                        PaddingValues(vertical = 12.dp)
                                ) {
                                    items(results) { show ->
                                        SearchShowCard(
                                            show = show,
                                            alreadyAdded = shows.any {
                                                it.id == show.id
                                            },
                                            onAdd = {
                                                if (shows.none {
                                                        it.id == show.id
                                                    }) {
                                                    shows = shows + show
                                                    Storage.saveShows(
                                                        this@MainActivity,
                                                        shows
                                                    )
                                                    results = results.filterNot {
                                                        it.id == show.id
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(
                                    horizontal = 18.dp,
                                    vertical = 8.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Text(
                                        "${shows.size} seriaali sinu kollektsioonis",
                                        color = SoftText
                                    )
                                }

                                if (shows.isEmpty()) {
                                    item {
                                        EmptyCard(
                                            "Sinu kollektsioon on tühi. Lisa esimene seriaal!"
                                        )
                                    }
                                }

                                items(shows) { show ->
                                    SearchShowCard(
                                        show = show,
                                        alreadyAdded = true,
                                        onAdd = {
                                            shows = shows.filterNot {
                                                it.id == show.id
                                            }
                                            Storage.saveShows(
                                                this@MainActivity,
                                                shows
                                            )
                                        },
                                        removeMode = true
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SectionTitle(title: String) {
        Text(
            title,
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold
        )
    }

    private fun showTitle(show: Show): String {
        return if (show.premieredYear != null) {
            "${show.name} (${show.premieredYear})"
        } else {
            show.name
        }
    }

    @Composable
    private fun ShowPosterCard(show: Show) {
        Column(
            modifier = Modifier.width(112.dp)
        ) {
            AsyncImage(
                model = show.imageUrl,
                contentDescription = show.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardBg),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(6.dp))

            Text(
                showTitle(show),
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    @Composable
    private fun SearchShowCard(
        show: Show,
        alreadyAdded: Boolean,
        onAdd: () -> Unit,
        removeMode: Boolean = false
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = CardBg
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = show.imageUrl,
                    contentDescription = show.name,
                    modifier = Modifier
                        .size(width = 68.dp, height = 94.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PageBg),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.width(12.dp))

                Text(
                    showTitle(show),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                TextButton(
                    onClick = onAdd,
                    enabled = removeMode || !alreadyAdded
                ) {
                    Text(
                        if (removeMode) "Eemalda"
                        else if (alreadyAdded) "Lisatud"
                        else "Lisa",
                        color = Accent
                    )
                }
            }
        }
    }

    @Composable
    private fun EpisodeCard(episode: Episode) {
        val date = runCatching {
            LocalDate.parse(episode.airdate).format(
                DateTimeFormatter.ofPattern("dd.MM.yyyy")
            )
        }.getOrDefault(episode.airdate)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = CardBg
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(15.dp)
            ) {
                Text(
                    episode.showName.uppercase(),
                    color = Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    episode.name,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    "$date   •   ${episode.season}. hooaeg  •   ${episode.number}. osa",
                    color = SoftText,
                    fontSize = 12.sp
                )

                if (episode.summary.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))

                    Text(
                        episode.summary,
                        color = SoftText,
                        fontSize = 12.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyCard(text: String) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = CardBg
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text,
                color = SoftText,
                modifier = Modifier.padding(18.dp)
            )
        }
    }
}
