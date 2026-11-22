package com.reel2playlist.recognition

import android.content.Context
import android.media.MediaRecorder
import com.reel2playlist.data.Song
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * AudD recognition adapter. Requires the user's own AudD API token.
 * Audio is uploaded to AudD; disclose this to the user before enabling recognition.
 */
class AuddRecognizer(private val context: Context) {
    fun recognize(file: File): Song {
        val token = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
            .getString("audd_token", null)?.trim().orEmpty()
        require(token.isNotBlank()) { "Add your AudD API token in Settings before recognizing songs." }

        val boundary = "----Reel2Playlist${System.currentTimeMillis()}"
        val conn = (URL("https://api.audd.io/").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        }
        conn.outputStream.use { out ->
            fun field(name: String, value: String) {
                out.write("--$boundary\r\n".toByteArray())
                out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
                out.write(value.toByteArray())
                out.write("\r\n".toByteArray())
            }
            field("api_token", token)
            field("return", "spotify")
            out.write("--$boundary\r\n".toByteArray())
            out.write("Content-Disposition: form-data; name=\"file\"; filename=\"sample.m4a\"\r\n".toByteArray())
            out.write("Content-Type: audio/mp4\r\n\r\n".toByteArray())
            file.inputStream().use { it.copyTo(out) }
            out.write("\r\n--$boundary--\r\n".toByteArray())
        }
        val body = (if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream)
            .bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val result = json.optJSONObject("result") ?: error(json.optString("error", "No matching song found."))
        val title = result.optString("title").takeIf { it.isNotBlank() } ?: error("Song title not returned.")
        val artist = result.optString("artist")
        val query = URLEncoder.encode("$title $artist", "UTF-8")
        return Song(title, artist, result.optString("album"), "https://open.spotify.com/search/$query")
    }

    companion object {
        fun recordSample(context: Context, seconds: Int = 12): File {
            val file = File(context.cacheDir, "reel-sample.m4a")
            @Suppress("DEPRECATION")
            val recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            Thread.sleep(seconds * 1000L)
            recorder.stop()
            recorder.release()
            return file
        }
    }
}
