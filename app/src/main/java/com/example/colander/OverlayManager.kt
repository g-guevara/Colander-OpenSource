package com.example.colander

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout

class OverlayManager(private val context: Context) {

    companion object {
        private const val TAG = "OverlayManager"
        private const val INITIAL_OVERLAY_DURATION = 200L // 200ms para overlay inicial blanco
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val handler = Handler(Looper.getMainLooper())

    // Views de overlay
    private var reelsOverlayView: View? = null
    private var feedOverlayView: View? = null
    private var searchOverlayView: View? = null

    // Overlay inicial blanco para evitar delay triggers
    private var initialWhiteOverlay: View? = null

    // Configuración del overlay con márgenes separados
    var topMarginFeed = 100
        private set
    var topMarginSearch = 150
        private set
    var bottomMargin = 100
        private set

    // Control de reintentos para overlays
    private var overlayRetryCount = 0
    private val maxOverlayRetries = 3

    fun updateSettings(topFeed: Int, topSearch: Int, bottom: Int) {
        topMarginFeed = topFeed
        topMarginSearch = topSearch
        bottomMargin = bottom
    }

    // ========== NUEVO: OVERLAY INICIAL BLANCO ==========

    /**
     * Muestra un overlay blanco de 100% width inmediatamente cuando Instagram se abre
     * para evitar que el usuario vea el contenido antes de que se aplique el bloqueo específico
     */
    fun showInitialWhiteOverlay() {
        try {
            if (initialWhiteOverlay != null) {
                hideInitialWhiteOverlay()
            }

            initialWhiteOverlay = createWhiteFullScreenView()

            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels

            val params = createOverlayParams(screenWidth, screenHeight)
            params.x = 0
            params.y = 0

            windowManager.addView(initialWhiteOverlay, params)
            Log.d(TAG, "⚪ Overlay blanco inicial mostrado (100% pantalla)")

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando overlay blanco inicial: ${e.message}")
        }
    }

    /**
     * Oculta el overlay blanco inicial
     */
    fun hideInitialWhiteOverlay() {
        initialWhiteOverlay?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "🗑️ Overlay blanco inicial eliminado")
            } catch (e: Exception) {
                Log.e(TAG, "Error eliminando overlay blanco inicial: ${e.message}")
            }
        }
        initialWhiteOverlay = null
    }

    // ========== OVERLAYS ESPECÍFICOS CON TRANSICIÓN ==========

    fun showReelsOverlay(coordinates: Rect) {
        try {
            // Primero mostrar overlay blanco si no está ya
            if (initialWhiteOverlay == null) {
                showInitialWhiteOverlay()
            }

            // Después de un delay corto, mostrar el overlay específico de Reels
            handler.postDelayed({
                showReelsOverlaySpecific(coordinates)
                hideInitialWhiteOverlay()
            }, INITIAL_OVERLAY_DURATION)

        } catch (e: Exception) {
            Log.e(TAG, "Error en transición de overlay Reels: ${e.message}")
            handleOverlayError()
        }
    }

    private fun showReelsOverlaySpecific(coordinates: Rect) {
        try {
            if (reelsOverlayView != null) {
                hideReelsOverlay()
            }

            reelsOverlayView = createRedSquareView()

            val buttonWidth = coordinates.width()
            val buttonHeight = coordinates.height()
            val overlayWidth = (buttonWidth * 0.8).toInt().coerceAtLeast(50)
            val overlayHeight = (buttonHeight * 0.8).toInt().coerceAtLeast(50)

            val params = createOverlayParams(overlayWidth, overlayHeight)
            params.x = coordinates.centerX() - overlayWidth / 2
            params.y = coordinates.centerY() - overlayHeight / 2

            windowManager.addView(reelsOverlayView, params)
            Log.d(TAG, "✅ Overlay Reels específico mostrado")
            overlayRetryCount = 0

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando overlay Reels específico: ${e.message}")
            handleOverlayError()
        }
    }

    fun showFeedTabOverlay() {
        try {
            // Primero mostrar overlay blanco si no está ya
            if (initialWhiteOverlay == null) {
                showInitialWhiteOverlay()
            }

            // Después de un delay corto, mostrar el overlay específico de Feed
            handler.postDelayed({
                showFeedTabOverlaySpecific()
                hideInitialWhiteOverlay()
            }, INITIAL_OVERLAY_DURATION)

        } catch (e: Exception) {
            Log.e(TAG, "Error en transición de overlay Feed: ${e.message}")
            handleOverlayError()
        }
    }

    private fun showFeedTabOverlaySpecific() {
        try {
            if (feedOverlayView != null) {
                hideFeedTabOverlay()
            }

            feedOverlayView = createRedSquareView()

            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val overlayWidth = screenWidth
            val overlayHeight = screenHeight - topMarginFeed - bottomMargin

            val params = createOverlayParams(overlayWidth, overlayHeight)
            params.x = 0
            params.y = topMarginFeed

            windowManager.addView(feedOverlayView, params)
            Log.d(TAG, "✅ Overlay Feed Tab específico mostrado (🏠 Superior: ${topMarginFeed}px, ⬇️ Inferior: ${bottomMargin}px)")
            overlayRetryCount = 0

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando overlay Feed Tab específico: ${e.message}")
            handleOverlayError()
        }
    }

    fun showSearchTabOverlay() {
        try {
            // Primero mostrar overlay blanco si no está ya
            if (initialWhiteOverlay == null) {
                showInitialWhiteOverlay()
            }

            // Después de un delay corto, mostrar el overlay específico de Search
            handler.postDelayed({
                showSearchTabOverlaySpecific()
                hideInitialWhiteOverlay()
            }, INITIAL_OVERLAY_DURATION)

        } catch (e: Exception) {
            Log.e(TAG, "Error en transición de overlay Search: ${e.message}")
            handleOverlayError()
        }
    }

    private fun showSearchTabOverlaySpecific() {
        try {
            if (searchOverlayView != null) {
                hideSearchTabOverlay()
            }

            searchOverlayView = createRedSquareView()

            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val overlayWidth = screenWidth
            val overlayHeight = screenHeight - topMarginSearch - bottomMargin

            val params = createOverlayParams(overlayWidth, overlayHeight)
            params.x = 0
            params.y = topMarginSearch

            windowManager.addView(searchOverlayView, params)
            Log.d(TAG, "✅ Overlay Search Tab específico mostrado (🔍 Superior: ${topMarginSearch}px, ⬇️ Inferior: ${bottomMargin}px)")
            overlayRetryCount = 0

        } catch (e: Exception) {
            Log.e(TAG, "Error mostrando overlay Search Tab específico: ${e.message}")
            handleOverlayError()
        }
    }

    // ========== MÉTODOS DE LIMPIEZA ==========

    fun hideReelsOverlay() {
        reelsOverlayView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "🗑️ Overlay Reels eliminado")
            } catch (e: Exception) {
                Log.e(TAG, "Error eliminando overlay Reels: ${e.message}")
            }
        }
        reelsOverlayView = null
    }

    fun hideFeedTabOverlay() {
        feedOverlayView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "🗑️ Overlay Feed Tab eliminado")
            } catch (e: Exception) {
                Log.e(TAG, "Error eliminando overlay Feed Tab: ${e.message}")
            }
        }
        feedOverlayView = null
    }

    fun hideSearchTabOverlay() {
        searchOverlayView?.let { view ->
            try {
                windowManager.removeView(view)
                Log.d(TAG, "🗑️ Overlay Search Tab eliminado")
            } catch (e: Exception) {
                Log.e(TAG, "Error eliminando overlay Search Tab: ${e.message}")
            }
        }
        searchOverlayView = null
    }

    fun hideAllOverlays() {
        hideInitialWhiteOverlay()
        hideReelsOverlay()
        hideFeedTabOverlay()
        hideSearchTabOverlay()

        // Cancelar cualquier handler pendiente
        handler.removeCallbacksAndMessages(null)
    }

    // ========== MÉTODOS AUXILIARES ==========

    private fun createOverlayParams(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun handleOverlayError() {
        overlayRetryCount++
        if (overlayRetryCount >= maxOverlayRetries) {
            Log.w(TAG, "⚠️ Máximo de reintentos alcanzado para overlays")
            overlayRetryCount = 0
        }
    }

    /**
     * Crea un overlay blanco de pantalla completa para transición inicial
     */
    private fun createWhiteFullScreenView(): View {
        return FrameLayout(context).apply {
            setBackgroundColor(android.graphics.Color.WHITE)

            isClickable = true
            isFocusable = false
            isFocusableInTouchMode = false

            setOnTouchListener { _, _ ->
                Log.d(TAG, "🚫 Touch bloqueado en overlay blanco inicial")
                true
            }

            setOnClickListener {
                Log.d(TAG, "🚫 Click bloqueado en overlay blanco inicial")
            }
        }
    }

    /**
     * Crea un overlay rojo para bloqueos específicos
     */
    private fun createRedSquareView(): View {
        return FrameLayout(context).apply {
            setBackgroundColor(android.graphics.Color.WHITE) // Cambié a blanco para consistencia

            isClickable = true
            isFocusable = false
            isFocusableInTouchMode = false

            setOnTouchListener { _, _ ->
                Log.d(TAG, "🚫 Touch bloqueado en overlay específico")
                true
            }

            setOnClickListener {
                Log.d(TAG, "🚫 Click bloqueado en overlay específico")
            }
        }
    }
}