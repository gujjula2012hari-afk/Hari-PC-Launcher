package com.hari.pclauncher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.StatFs
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var exoPlayer: ExoPlayer? = null
    private lateinit var playerView: PlayerView
    private lateinit var startMenu: LinearLayout
    private lateinit var pcWindow: LinearLayout
    private lateinit var windowTitle: TextView
    private lateinit var windowContent: TextView
    private lateinit var txtClock: TextView

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            getSharedPreferences("HariLauncherPrefs", Context.MODE_PRIVATE)
                .edit().putString("wallpaper_uri", it.toString()).apply()
            playVideoWallpaper(it)
            Toast.makeText(this, "4K Live Video Wallpaper Applied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        playerView = findViewById(R.id.videoWallpaperView)
        startMenu = findViewById(R.id.startMenu)
        pcWindow = findViewById(R.id.pcWindow)
        windowTitle = findViewById(R.id.windowTitle)
        windowContent = findViewById(R.id.windowContent)
        txtClock = findViewById(R.id.txtClock)

        setup4KVideoEngine()
        setupDesktopIcons()
        setupStartMenuAndTaskbar()
        startClock()
    }

    private fun setup4KVideoEngine() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
        }
        playerView.player = exoPlayer

        val savedUri = getSharedPreferences("HariLauncherPrefs", Context.MODE_PRIVATE)
            .getString("wallpaper_uri", null)
        if (savedUri != null) {
            playVideoWallpaper(Uri.parse(savedUri))
        }
    }

    private fun playVideoWallpaper(uri: Uri) {
        try {
            val mediaItem = MediaItem.fromUri(uri)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.play()
        } catch (e: Exception) {
            Toast.makeText(this, "Select a valid 4K/HD video file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDesktopIcons() {
        findViewById<TextView>(R.id.btnThisPC).setOnClickListener {
            val stat = StatFs(Environment.getDataDirectory().path)
            val totalGB = (stat.blockCountLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
            val freeGB = (stat.availableBlocksLong * stat.blockSizeLong) / (1024 * 1024 * 1024)
            val usedGB = totalGB - freeGB

            openPCWindow(
                "💻 This PC - Local Disk (C:)",
                "Device: ${android.os.Build.MODEL}\n" +
                "OS: Hari PC OS (Android ${android.os.Build.VERSION.RELEASE})\n\n" +
                "💽 Local Disk (C:)\n" +
                "Total Space: $totalGB GB\n" +
                "Used Space: $usedGB GB\n" +
                "Free Space: $freeGB GB\n\n" +
                "📁 Desktop\n📁 Documents\n📁 Downloads\n📁 4K Videos"
            )
        }

        findViewById<TextView>(R.id.btnWallpaper).setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }

        findViewById<TextView>(R.id.btnTaskManager).setOnClickListener {
            val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            val totalRam = memInfo.totalMem / (1024 * 1024)
            val availRam = memInfo.availMem / (1024 * 1024)
            val usedRam = totalRam - availRam

            openPCWindow(
                "📊 Task Manager - Performance",
                "CPU Cores: ${Runtime.getRuntime().availableProcessors()} Cores Active\n" +
                "Memory (RAM): $usedRam MB / $totalRam MB Used\n" +
                "Available RAM: $availRam MB\n\n" +
                "Status: Running Smoothly (4K Hardware Acceleration ON)"
            )
        }

        findViewById<TextView>(R.id.btnControlPanel).setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }

        findViewById<TextView>(R.id.btnRecycleBin).setOnClickListener {
            openPCWindow(
                "🗑️ Recycle Bin",
                "Recycle Bin is currently empty.\n\nDeleted desktop shortcuts and temporary files will appear here."
            )
        }

        findViewById<TextView>(R.id.btnCloseWindow).setOnClickListener {
            pcWindow.visibility = View.GONE
        }
    }

    private fun openPCWindow(title: String, content: String) {
        startMenu.visibility = View.GONE
        windowTitle.text = title
        windowContent.text = content
        pcWindow.visibility = View.VISIBLE
    }

    private fun setupStartMenuAndTaskbar() {
        findViewById<TextView>(R.id.btnStart).setOnClickListener {
            pcWindow.visibility = View.GONE
            startMenu.visibility = if (startMenu.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        findViewById<TextView>(R.id.btnQuickCalc).setOnClickListener {
            openPCWindow("🧮 PC Calculator", "Quick Calculator Ready.\n\nTip: Use Start Menu to launch full Android Calculator app.")
        }

        findViewById<TextView>(R.id.btnQuickNote).setOnClickListener {
            openPCWindow("📝 Hari Notepad", "Welcome to Hari PC Launcher!\n\nClick '4K Video' on Desktop to set any 4K live video wallpaper.")
        }

        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val appNames = apps.map { it.loadLabel(pm).toString() }
        val listView = findViewById<ListView>(R.id.appListView)
        listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, appNames)

        listView.setOnItemClickListener { _, _, position, _ ->
            val launchIntent = pm.getLaunchIntentForPackage(apps[position].activityInfo.packageName)
            if (launchIntent != null) {
                startMenu.visibility = View.GONE
                startActivity(launchIntent)
            }
        }
    }

    private fun startClock() {
        val handler = Handler(Looper.getMainLooper())
        val sdf = SimpleDateFormat("hh:mm a\ndd-MM-yyyy", Locale.getDefault())
        handler.post(object : Runnable {
            override fun run() {
                txtClock.text = sdf.format(Date())
                handler.postDelayed(this, 1000)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
    }
}
