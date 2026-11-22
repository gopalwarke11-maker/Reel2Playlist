package com.reel2playlist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.reel2playlist.data.Song
import com.reel2playlist.data.SongHistory
import com.reel2playlist.overlay.FloatingButtonService
import com.reel2playlist.recognition.AuddRecognizer
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var history: SongHistory
    private var recognitionRequested = false
    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecognition() else status.value = "Microphone permission is required."
    }
    private val status = mutableStateOf("Ready. Tap the floating button to identify a song.")
    private val songsState = mutableStateOf<List<Song>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        history = SongHistory(this)
        songsState.value = history.all()
        handleIntent(intent)
        setContent {
            MaterialTheme {
                val songs by songsState
                var token by remember { mutableStateOf(getSharedPreferences("settings", MODE_PRIVATE).getString("audd_token", "") ?: "") }
                var overlayText by remember { mutableStateOf("Enable floating button") }
                Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Reel2Playlist", style = MaterialTheme.typography.headlineLarge)
                    Text("Recognize a song. Open it in Spotify.", style = MaterialTheme.typography.bodyMedium)
                    Card {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Recognition setup", style = MaterialTheme.typography.titleMedium)
                            Text("This free app version uses AudD for song recognition. You need your own AudD API token. A short microphone recording is uploaded to AudD when you identify a song.")
                            OutlinedTextField(
                                value = token, onValueChange = { token = it },
                                label = { Text("AudD API token") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(onClick = {
                                getSharedPreferences("settings", MODE_PRIVATE).edit().putString("audd_token", token.trim()).apply()
                                status.value = "Recognition token saved on this device."
                            }) { Text("Save token") }
                        }
                    }
                    Button(onClick = {
                        if (!Settings.canDrawOverlays(this@MainActivity)) {
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        } else {
                            ContextCompat.startForegroundService(this@MainActivity, Intent(this@MainActivity, FloatingButtonService::class.java))
                            overlayText = "Floating button enabled"
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text(overlayText) }
                    Button(onClick = { requestRecognition() }, modifier = Modifier.fillMaxWidth()) { Text("Identify song now") }
                    Text(status.value, color = MaterialTheme.colorScheme.secondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Song history", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { history.clear(); songs = emptyList() }) { Text("Clear") }
                    }
                    if (songs.isEmpty()) Text("No songs recognized yet.")
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(songs) { song ->
                            Card {
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(song.title, style = MaterialTheme.typography.titleMedium)
                                    Text(song.artist)
                                    Text(DateFormat.getDateTimeInstance().format(Date(song.recognizedAt)), style = MaterialTheme.typography.labelSmall)
                                    TextButton(onClick = { openSpotify(song) }) { Text("Open in Spotify") }
                                }
                            }
                        }
                    }
                }
                LaunchedEffect(Unit) {
                    if (recognitionRequested) { recognitionRequested = false; requestRecognition() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        if (recognitionRequested) {
            recognitionRequested = false
            requestRecognition()
        }
    }

    private fun handleIntent(intent: Intent?) {
        recognitionRequested = intent?.getBooleanExtra("start_recognition", false) == true
    }

    private fun requestRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startRecognition()
        } else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startRecognition() {
        status.value = "Listening for up to 12 seconds…"
        executor.execute {
            try {
                val file = AuddRecognizer.recordSample(this, 12)
                val song = AuddRecognizer(this).recognize(file)
                history.add(song)
                runOnUiThread { songsState.value = history.all(); status.value = "Found: ${song.title} — ${song.artist}" }
            } catch (e: Exception) {
                runOnUiThread { status.value = e.message ?: "Recognition failed." }
            }
        }
    }

    private fun openSpotify(song: Song) {
        val appUri = Uri.parse("spotify:search:${Uri.encode(song.title + " " + song.artist)}")
        val webUri = Uri.parse(song.spotifySearchUrl)
        try {
            startActivity(Intent(Intent.ACTION_VIEW, appUri))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }
}
