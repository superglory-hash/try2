package com.seoul.fmonair

import android.content.ComponentName
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.seoul.fmonair.data.*
import com.seoul.fmonair.service.RadioPlaybackService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionToken = SessionToken(this, ComponentName(this, RadioPlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()

        setContent {
            SeoulRadioTheme {
                MainScreen(
                    onPlayStation = { station, onResolved ->
                        lifecycleScopeLaunch {
                            val resolvedUrl = StationRepository.resolvePlayableStreamUrl(station)
                            if (resolvedUrl != null) {
                                mediaController?.let { player ->
                                    val mediaItem = MediaItem.Builder()
                                        .setUri(Uri.parse(resolvedUrl))
                                        .setMediaId(station.id)
                                        .build()
                                    player.setMediaItem(mediaItem)
                                    player.prepare()
                                    player.play()
                                }
                                onResolved(true)
                            } else {
                                Toast.makeText(this@MainActivity, "스트림 주소를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                                onResolved(false)
                            }
                        }
                    },
                    onTogglePlay = {
                        mediaController?.let {
                            if (it.isPlaying) it.pause() else it.play()
                        }
                    }
                )
            }
        }

        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun lifecycleScopeLaunch(block: suspend () -> Unit) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            block()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}

@Composable
fun SeoulRadioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0B1120),
            surface = Color(0xFF111827),
            primary = Color(0xFF6366F1),
            secondary = Color(0xFF06B6D4)
        ),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onPlayStation: (RadioStation, (Boolean) -> Unit) -> Unit,
    onTogglePlay: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val stations = remember { mutableStateListOf(*StationRepository.defaultStations.toTypedArray()) }
    var currentStationIndex by remember { mutableStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(StationCategory.ALL) }
    var currentTab by remember { mutableStateOf(0) }

    // 가사 검색 관련 상태
    var lyricsQuery by remember { mutableStateOf("") }
    var lyricsResult by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isSearchingLyrics by remember { mutableStateOf(false) }

    val currentStation = stations.getOrNull(currentStationIndex) ?: stations[0]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Radio, contentDescription = null, tint = Color(0xFF818CF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("수도권 온에어 FM", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B1120))
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF111827)) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.List, contentDescription = "채널") },
                    label = { Text("채널") }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.MusicNote, contentDescription = "가사") },
                    label = { Text("가사") }
                )
            }
        },
        containerColor = Color(0xFF0B1120)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Now Playing Hero Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF6366F1).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = currentStation.category.label,
                                    color = Color(0xFF818CF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(currentStation.name, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                            Text("\${currentStation.frequency} MHz · \${currentStation.description}", fontSize = 12.sp, color = Color.LightGray)
                        }

                        // 이퀄라이저 시각화
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            repeat(5) { i ->
                                val transition = rememberInfiniteTransition()
                                val heightAnim by transition.animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = if (isPlaying) 1.0f else 0.2f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(400 + i * 150, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight(heightAnim)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(Color(0xFF06B6D4))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 컨트롤러
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            currentStationIndex = (currentStationIndex - 1 + stations.size) % stations.size
                            isLoading = true
                            onPlayStation(stations[currentStationIndex]) {
                                isLoading = false
                                isPlaying = it
                            }
                        }) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "이전 채널", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        FloatingActionButton(
                            onClick = {
                                if (isPlaying) {
                                    onTogglePlay()
                                    isPlaying = false
                                } else {
                                    isLoading = true
                                    onPlayStation(currentStation) { success ->
                                        isLoading = false
                                        isPlaying = success
                                    }
                                }
                            },
                            containerColor = Color(0xFF6366F1),
                            contentColor = Color.White,
                            shape = CircleShape
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "재생/정지")
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(onClick = {
                            currentStationIndex = (currentStationIndex + 1) % stations.size
                            isLoading = true
                            onPlayStation(stations[currentStationIndex]) {
                                isLoading = false
                                isPlaying = it
                            }
                        }) {
                            Icon(Icons.Default.SkipNext, contentDescription = "다음 채널", tint = Color.White)
                        }
                    }
                }
            }

            if (currentTab == 0) {
                // 채널 탭
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    items(StationCategory.values()) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label, fontSize = 12.sp) }
                        )
                    }
                }

                val filteredStations = remember(selectedCategory, stations) {
                    if (selectedCategory == StationCategory.ALL) stations
                    else if (selectedCategory == StationCategory.FAVORITES) stations.filter { it.isFavorite }
                    else stations.filter { it.category == selectedCategory }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredStations) { station ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    currentStationIndex = stations.indexOf(station)
                                    isLoading = true
                                    onPlayStation(station) { success ->
                                        isLoading = false
                                        isPlaying = success
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("\${station.frequency} MHz", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(station.name, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 15.sp)
                                    }
                                    Text(station.description, color = Color.Gray, fontSize = 12.sp)
                                }
                                IconButton(onClick = {
                                    val index = stations.indexOf(station)
                                    if (index != -1) {
                                        val updated = stations[index].copy(isFavorite = !stations[index].isFavorite)
                                        stations[index] = updated
                                    }
                                }) {
                                    Icon(
                                        if (station.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = "즐겨찾기",
                                        tint = if (station.isFavorite) Color(0xFFFBBF24) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // 가사 검색 탭
                Column(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = lyricsQuery,
                            onValueChange = { lyricsQuery = it },
                            placeholder = { Text("곡명 또는 가수 검색...", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (lyricsQuery.isNotBlank()) {
                                    isSearchingLyrics = true
                                    coroutineScope.launch {
                                        lyricsResult = SongAndLyricsService.searchLyrics(lyricsQuery)
                                        isSearchingLyrics = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                        ) {
                            Text("검색")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxSize().padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp).fillMaxSize()) {
                            if (isSearchingLyrics) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFF6366F1))
                                }
                            } else if (lyricsResult != null) {
                                Text(lyricsResult!!.first, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF38BDF8))
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item {
                                        Text(
                                            lyricsResult!!.second,
                                            color = Color.LightGray,
                                            fontSize = 14.sp,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("검색할 곡명이나 아티스트를 입력하세요.", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
