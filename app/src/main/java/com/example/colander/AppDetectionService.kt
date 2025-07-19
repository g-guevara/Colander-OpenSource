package com.example.colander

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class AppDetectionService : Service() {

    companion object {
        private const val TAG = "AppDetectionService"
        private const val CHANNEL_ID = "AppDetectionChannel"
        private const val NOTIFICATION_ID = 1
        private const val MIN_CHECK_INTERVAL = 1000L
        private const val MAX_CHECK_INTERVAL = 3000L
        private const val INSTAGRAM_DETECTION_DELAY = 100L // Delay mínimo para mostrar overlay inicial
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var overlayManager: OverlayManager
    private lateinit var elementStateManager: ElementStateManager

    private val handler = Handler(Looper.getMainLooper())
    private var isInstagramOpen = false
    private var currentCheckInterval = MIN_CHECK_INTERVAL

    // Estados de Instagram
    private var isInDirectMessages = false

    // Control de estado previo para evitar operaciones innecesarias
    private var previousInstagramState = false

    private val instagramPackages = setOf("com.instagram.android")

    private val overlaySettingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.colander.OVERLAY_SETTINGS_UPDATE") {
                val topMarginFeed = intent.getIntExtra("top_margin_feed", 100)
                val topMarginSearch = intent.getIntExtra("top_margin_search", 150)
                val bottomMargin = intent.getIntExtra("bottom_margin", 100)

                overlayManager.updateSettings(topMarginFeed, topMarginSearch, bottomMargin)

                Log.d(TAG, "📐 Configuración actualizada:")
                Log.d(TAG, "   🏠 Feed Superior: ${topMarginFeed}px")
                Log.d(TAG, "   🔍 Search Superior: ${topMarginSearch}px")
                Log.d(TAG, "   ⬇️ Inferior compartido: ${bottomMargin}px")

                // Actualizar overlays si estamos en feed o search
                if (isInstagramOpen && (elementStateManager.isInFeedTab || elementStateManager.isInSearchTab)) {
                    updateOverlaysWithDelay()
                }
                updateNotification()
            }
        }
    }

    private val instagramElementsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent?.action == "com.example.colander.INSTAGRAM_ELEMENTS_UPDATE") {
                    handleInstagramElementsUpdate(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando actualización de elementos: ${e.message}")
            }
        }
    }

    private val backButtonReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent?.action == "com.example.colander.BACK_BUTTON_DETECTED") {
                    val backButtonVisible = intent.getBooleanExtra("back_button_visible", false)

                    if (backButtonVisible) {
                        Log.d(TAG, "🔙 Botón BACK detectado - Cancelando overlays excepto navtab")
                        handleBackButtonDetection()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error procesando detección de botón back: ${e.message}")
            }
        }
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            try {
                checkForegroundApp()
                val nextInterval = if (isInstagramOpen) MIN_CHECK_INTERVAL else MAX_CHECK_INTERVAL
                if (nextInterval != currentCheckInterval) {
                    currentCheckInterval = nextInterval
                    Log.d(TAG, "Intervalo ajustado a: ${currentCheckInterval}ms")
                }
                handler.postDelayed(this, currentCheckInterval)
            } catch (e: Exception) {
                Log.e(TAG, "Error en checkRunnable: ${e.message}")
                handler.postDelayed(this, MAX_CHECK_INTERVAL)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        try {
            prefs = getSharedPreferences("cube_settings", Context.MODE_PRIVATE)
            overlayManager = OverlayManager(this)
            elementStateManager = ElementStateManager()

            createNotificationChannel()
            loadOverlaySettings()
            registerReceivers()
            Log.d(TAG, "✅ AppDetectionService creado exitosamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}")
        }
    }

    private fun registerReceivers() {
        try {
            val filter = IntentFilter("com.example.colander.INSTAGRAM_ELEMENTS_UPDATE")
            val overlayFilter = IntentFilter("com.example.colander.OVERLAY_SETTINGS_UPDATE")
            val backButtonFilter = IntentFilter("com.example.colander.BACK_BUTTON_DETECTED")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(instagramElementsReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                registerReceiver(overlaySettingsReceiver, overlayFilter, Context.RECEIVER_NOT_EXPORTED)
                registerReceiver(backButtonReceiver, backButtonFilter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(instagramElementsReceiver, filter)
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(overlaySettingsReceiver, overlayFilter)
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(backButtonReceiver, backButtonFilter)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registrando receivers: ${e.message}")
        }
    }

    private fun loadOverlaySettings() {
        val topMarginFeed = prefs.getInt("top_margin_feed", 100)
        val topMarginSearch = prefs.getInt("top_margin_search", 150)
        val bottomMargin = prefs.getInt("bottom_margin", 100)

        overlayManager.updateSettings(topMarginFeed, topMarginSearch, bottomMargin)

        Log.d(TAG, "📐 Configuración cargada:")
        Log.d(TAG, "   🏠 Feed Superior: ${topMarginFeed}px")
        Log.d(TAG, "   🔍 Search Superior: ${topMarginSearch}px")
        Log.d(TAG, "   ⬇️ Inferior compartido: ${bottomMargin}px")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            startMonitoring()
            Log.d(TAG, "🚀 Servicio iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onStartCommand: ${e.message}")
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoring() {
        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable)
    }

    private fun checkForegroundApp() {
        val currentApp = getForegroundApp()
        val shouldShowInstagram = currentApp in instagramPackages

        if (shouldShowInstagram != previousInstagramState) {
            previousInstagramState = shouldShowInstagram

            if (shouldShowInstagram && !isInstagramOpen) {
                isInstagramOpen = true
                Log.d(TAG, "📱 Instagram ABIERTO")

                // NUEVO: Mostrar overlay blanco inicial inmediatamente
                showInitialOverlayWithDelay()

            } else if (!shouldShowInstagram && isInstagramOpen) {
                isInstagramOpen = false
                Log.d(TAG, "❌ Instagram CERRADO")
                overlayManager.hideAllOverlays()
                elementStateManager.clearState()
            }
        }
    }

    /**
     * NUEVO: Muestra overlay blanco inicial inmediatamente cuando Instagram se abre
     */
    private fun showInitialOverlayWithDelay() {
        handler.postDelayed({
            if (isInstagramOpen) {
                Log.d(TAG, "⚪ Mostrando overlay blanco inicial para Instagram")
                overlayManager.showInitialWhiteOverlay()

                // Después de mostrar el overlay inicial, proceder con la detección normal
                updateOverlaysWithDelay()
            }
        }, INSTAGRAM_DETECTION_DELAY)
    }

    private fun getForegroundApp(): String? {
        return try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val currentTime = System.currentTimeMillis()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                currentTime - 10000,
                currentTime
            )
            stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo app en primer plano: ${e.message}")
            null
        }
    }

    private fun handleBackButtonDetection() {
        try {
            // Cancelar overlays de navtab (Feed y Search)
            overlayManager.hideFeedTabOverlay()
            overlayManager.hideSearchTabOverlay()

            // Actualizar el estado del botón back
            elementStateManager.isBackButtonVisible = true

            Log.d(TAG, "🔙 Botón back procesado - Navtab overlays (Feed/Search) cancelados, Reels overlay mantenido")
            updateNotification()

        } catch (e: Exception) {
            Log.e(TAG, "Error manejando detección de botón back: ${e.message}")
        }
    }

    private fun handleInstagramElementsUpdate(intent: Intent) {
        val hasChanges = elementStateManager.updateFromIntent(intent)

        // Detectar directos por URL o elementos específicos
        val currentActivity = intent.getStringExtra("current_activity") ?: ""
        isInDirectMessages = currentActivity.contains("direct") ||
                currentActivity.contains("message") ||
                intent.getBooleanExtra("in_direct_messages", false)

        Log.d(TAG, "Estados - Feed: ${elementStateManager.isInFeedTab}, Search: ${elementStateManager.isInSearchTab}, Teclado: ${elementStateManager.isKeyboardActiveInSearch}, Directos: $isInDirectMessages, Reels: ${elementStateManager.isReelsButtonVisible}, Back: ${elementStateManager.isBackButtonVisible}")

        if (hasChanges && isInstagramOpen) {
            updateOverlaysWithDelay()
        }
    }

    private fun updateOverlaysWithDelay() {
        handler.removeCallbacks(updateOverlaysRunnable)
        handler.postDelayed(updateOverlaysRunnable, 300)
    }

    private val updateOverlaysRunnable = Runnable {
        updateOverlays()
    }

    private fun updateOverlays() {
        try {
            // Solo actualizar si no estamos en directos
            if (isInDirectMessages) {
                Log.d(TAG, "📩 En directos - manteniendo overlays desactivados")
                overlayManager.hideAllOverlays()
                updateNotification()
                return
            }

            // Overlay del botón de Reels - funciona normalmente (NO se afecta por botón back)
            if (elementStateManager.isReelsButtonVisible && elementStateManager.reelsButtonCoordinates != null) {
                overlayManager.showReelsOverlay(elementStateManager.reelsButtonCoordinates!!)
            } else {
                overlayManager.hideReelsOverlay()
            }

            // Overlay del feed_tab - NO mostrar si botón back está visible
            if (elementStateManager.isInFeedTab && !elementStateManager.isBackButtonVisible) {
                overlayManager.showFeedTabOverlay()
                overlayManager.hideSearchTabOverlay()
            } else {
                overlayManager.hideFeedTabOverlay()
                if (elementStateManager.isBackButtonVisible && elementStateManager.isInFeedTab) {
                    Log.d(TAG, "🔙 Botón back detectado - NO mostrando overlay de Feed (navtab)")
                }
            }

            // Overlay del search_tab - NO mostrar si botón back está visible
            if (elementStateManager.isInSearchTab && !elementStateManager.isBackButtonVisible) {
                if (elementStateManager.isKeyboardActiveInSearch) {
                    Log.d(TAG, "⌨️ Teclado activo en Search - NO mostrando overlay")
                    overlayManager.hideSearchTabOverlay()
                } else {
                    Log.d(TAG, "🔍 Search sin teclado - mostrando overlay")
                    overlayManager.showSearchTabOverlay()
                }
                overlayManager.hideFeedTabOverlay()
            } else {
                overlayManager.hideSearchTabOverlay()
                if (elementStateManager.isBackButtonVisible && elementStateManager.isInSearchTab) {
                    Log.d(TAG, "🔙 Botón back detectado - NO mostrando overlay de Search (navtab)")
                }
            }

            updateNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando overlays: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Instagram Blocker",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Bloquea contenido de Instagram"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val statusText = when {
            isInDirectMessages -> "Instagram - En directos (sin bloqueo)"
            isInstagramOpen && elementStateManager.isInFeedTab && !elementStateManager.isBackButtonVisible -> "Instagram - Feed bloqueado (🏠${overlayManager.topMarginFeed}px-${overlayManager.bottomMargin}px)"
            isInstagramOpen && elementStateManager.isInFeedTab && elementStateManager.isBackButtonVisible -> "Instagram - Feed libre (🔙 back activo)"
            isInstagramOpen && elementStateManager.isInSearchTab && elementStateManager.isKeyboardActiveInSearch -> "Instagram - Search libre (⌨️ teclado activo)"
            isInstagramOpen && elementStateManager.isInSearchTab && !elementStateManager.isBackButtonVisible -> "Instagram - Search bloqueado (🔍${overlayManager.topMarginSearch}px-${overlayManager.bottomMargin}px)"
            isInstagramOpen && elementStateManager.isInSearchTab && elementStateManager.isBackButtonVisible -> "Instagram - Search libre (🔙 back activo)"
            isInstagramOpen && elementStateManager.isReelsButtonVisible -> "Instagram - Reels bloqueado"
            isInstagramOpen -> "Instagram abierto (⚪ overlay inicial)"
            else -> "Esperando Instagram"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Instagram Blocker")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        try {
            val notificationManager = getSystemService(NotificationManager::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando notificación: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            handler.removeCallbacks(checkRunnable)
            handler.removeCallbacks(updateOverlaysRunnable)
            overlayManager.hideAllOverlays()
            unregisterReceiver(instagramElementsReceiver)
            unregisterReceiver(overlaySettingsReceiver)
            unregisterReceiver(backButtonReceiver)
            Log.d(TAG, "💀 AppDetectionService destruido")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onDestroy: ${e.message}")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        val restartServiceIntent = Intent(applicationContext, AppDetectionService::class.java)
        startService(restartServiceIntent)
    }}