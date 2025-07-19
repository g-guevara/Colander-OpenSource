package com.example.colander


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.app.NotificationCompat

class SimpleAppDetectionService : Service() {

    companion object {
        private const val TAG = "SimpleDetection"
        private const val CHANNEL_ID = "SimpleDetectionChannel"
        private const val NOTIFICATION_ID = 1
    }

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isInstagramOpen = false
    private val checkInterval = 2000L

    private val instagramPackages = setOf("com.instagram.android")

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, checkInterval)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        Log.d(TAG, "✅ Servicio simple creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🚀 Servicio simple iniciado")
        startForeground(NOTIFICATION_ID, createNotification())
        startMonitoring()
        showToast("Servicio simple iniciado")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring() {
        handler.post(checkRunnable)
    }

    private fun checkForegroundApp() {
        val currentApp = getForegroundApp()
        val shouldShowInstagram = currentApp in instagramPackages

        Log.d(TAG, "🔍 App actual: $currentApp")

        if (shouldShowInstagram && !isInstagramOpen) {
            isInstagramOpen = true
            Log.d(TAG, "📱 Instagram ABIERTO")
            showToast("Instagram detectado - Mostrando overlay")
            showSimpleOverlay()
        } else if (!shouldShowInstagram && isInstagramOpen) {
            isInstagramOpen = false
            Log.d(TAG, "❌ Instagram CERRADO")
            showToast("Instagram cerrado - Ocultando overlay")
            hideOverlay()
        }
    }

    private fun getForegroundApp(): String? {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 1000 * 10,
                currentTime
            )

            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo app: ${e.message}")
            null
        }
    }

    private fun showSimpleOverlay() {
        if (overlayView != null) {
            hideOverlay()
        }

        overlayView = createRedSquareView()

        val params = WindowManager.LayoutParams(
            200, // ancho
            200, // alto
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER

        try {
            windowManager.addView(overlayView, params)
            Log.d(TAG, "✅ Overlay simple mostrado en el centro")
            showToast("Overlay mostrado exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Error mostrando overlay: ${e.message}")
            showToast("Error: ${e.message}")
        }
    }

    private fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "🗑️ Overlay eliminado")
            } catch (e: Exception) {
                Log.e(TAG, "Error eliminando overlay: ${e.message}")
            }
        }
        overlayView = null
    }

    private fun createRedSquareView(): View {
        val frameLayout = FrameLayout(this)
        frameLayout.setBackgroundColor(android.graphics.Color.RED)
        return frameLayout
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Simple Instagram Detector",
                NotificationManager.IMPORTANCE_LOW
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Simple Instagram Detector")
            .setContentText("Instagram: ${if(isInstagramOpen) "ABIERTO" else "CERRADO"}")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checkRunnable)
        hideOverlay()
        Log.d(TAG, "💀 Servicio destruido")
    }
}