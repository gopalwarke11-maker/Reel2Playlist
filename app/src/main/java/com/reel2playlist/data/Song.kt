package com.reel2playlist.data

data class Song(
    val title: String,
    val artist: String,
    val album: String = "",
    val spotifySearchUrl: String,
    val recognizedAt: Long = System.currentTimeMillis()
)
