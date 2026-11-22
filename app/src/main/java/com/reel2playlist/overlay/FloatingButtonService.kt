package com.reel2playlist.overlay

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.reel2playlist.MainActivity

class FloatingButtonService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null

    override fun onCreate() {
        super.onCreate()
        createNotification()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = TextView(this).apply {
            text = "♫"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(0xFFFFFFFF.toInt())
            setBackgroundColor(0xFF1DB954.toInt())
            setOnClickListener {
                val intent = Intent(this@FloatingButtonService, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    .putExtra("start_recognition", true)
                startActivity(intent)
            }
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            58, 58, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL; x = 18 }
        wm.addView(view, params)
        bubble = view
    }

    private fun createNotification() {
        val id = "reel2playlist"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(id, "Reel2Playlist", NotificationManager.IMPORTANCE_LOW))
        }
        val n = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, id).setContentTitle("Reel2Playlist active")
                .setContentText("Tap the floating music button to identify a song")
                .setSmallIcon(android.R.drawable.ic_media_play).build()
        else @Suppress("DEPRECATION") Notification.Builder(this)
            .setContentTitle("Reel2Playlist active").setContentText("Tap the floating music button")
            .setSmallIcon(android.R.drawable.ic_media_play).build()
        startForeground(1001, n)
    }

    override fun onDestroy() {
        bubble?.let { wm.removeView(it) }
        bubble = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
