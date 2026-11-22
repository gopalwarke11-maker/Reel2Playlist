package com.reel2playlist.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class SongHistory(context: Context) {
    private val prefs = context.getSharedPreferences("song_history", Context.MODE_PRIVATE)

    fun all(): List<Song> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).mapNotNull { i ->
            runCatching {
                val o = arr.getJSONObject(i)
                Song(
                    title = o.getString("title"),
                    artist = o.getString("artist"),
                    album = o.optString("album"),
                    spotifySearchUrl = o.getString("url"),
                    recognizedAt = o.optLong("at")
                )
            }.getOrNull()
        }
    }

    fun add(song: Song) {
        val existing = all().filterNot {
            it.title.equals(song.title, true) && it.artist.equals(song.artist, true)
        }
        val updated = (listOf(song) + existing).take(100)
        val arr = JSONArray()
        updated.forEach { s ->
            arr.put(JSONObject().put("title", s.title).put("artist", s.artist)
                .put("album", s.album).put("url", s.spotifySearchUrl).put("at", s.recognizedAt))
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    fun clear() { prefs.edit().remove("items").apply() }
}
