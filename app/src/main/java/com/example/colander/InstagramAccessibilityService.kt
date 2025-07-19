package com.example.colander

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class InstagramAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "InstagramAccessibility"
        var instance: InstagramAccessibilityService? = null
        var reelsButtonCoordinates: Rect? = null
        var isReelsButtonVisible = false
        var feedCoordinates: Rect? = null
        var isInFeedTab = false
        var isInSearchTab = false
        var isInDirectMessages = false
        var isKeyboardActiveInSearch = false
        var isBackButtonVisible = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var elementDetector: InstagramElementDetector
    private lateinit var contextAnalyzer: InstagramContextAnalyzer

    // Control de frecuencia para evitar spam
    private var lastDetectionTime = 0L
    private val minDetectionInterval = 500L

    // Cache de estado previo para evitar notificaciones innecesarias
    private var previousReelsState = false
    private var previousFeedState = false
    private var previousSearchState = false
    private var previousDirectState = false
    private var previousKeyboardState = false
    private var previousBackButtonState = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        elementDetector = InstagramElementDetector()
        contextAnalyzer = InstagramContextAnalyzer()
        Log.d(TAG, "✅ Instagram AccessibilityService conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName != "com.instagram.android") return

        // Control de frecuencia
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDetectionTime < minDetectionInterval) {
            return
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                if (shouldProcessEvent(event)) {
                    lastDetectionTime = currentTime
                    detectInstagramElementsOptimized()
                }
            }
        }
    }

    // Determinar si vale la pena procesar el evento
    private fun shouldProcessEvent(event: AccessibilityEvent?): Boolean {
        if (event == null) return false

        val className = event.className?.toString()
        val ignoredClasses = setOf(
            "android.widget.ProgressBar",
            "android.widget.SeekBar",
            "android.view.ViewGroup"
        )

        return className !in ignoredClasses
    }

    private fun detectInstagramElementsOptimized() {
        val rootNode = rootInActiveWindow ?: return

        try {
            // Detectar el botón de retroceso
            val backButton = elementDetector.findBackButtonOptimized(rootNode)
            val backButtonVisible = backButton != null

            // Si hay cambio en el estado del botón back, notificar inmediatamente
            if (backButtonVisible != previousBackButtonState) {
                isBackButtonVisible = backButtonVisible
                previousBackButtonState = backButtonVisible

                if (backButtonVisible) {
                    Log.d(TAG, "🔙 Botón BACK detectado - Cancelando overlays no-navtab")
                    notifyBackButtonDetected()
                } else {
                    Log.d(TAG, "❌ Botón BACK ya no visible")
                }
            }

            // Detectar contexto de Instagram
            val currentContext = contextAnalyzer.detectInstagramContext(rootNode)

            // Solo buscar elementos relevantes según el contexto
            when (currentContext) {
                InstagramContext.DIRECT_MESSAGES -> {
                    handleDirectMessagesContext()
                }
                InstagramContext.FEED -> {
                    detectFeedElements(rootNode)
                }
                InstagramContext.SEARCH -> {
                    detectSearchElements(rootNode)
                }
                InstagramContext.OTHER -> {
                    detectGeneralElements(rootNode)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Error detectando elementos: ${e.message}")
        } finally {
            rootNode.recycle()
        }
    }

    private fun handleDirectMessagesContext() {
        val newState = DetectionState(
            reelsVisible = false,
            inFeedTab = false,
            inSearchTab = false,
            inDirectMessages = true,
            keyboardActiveInSearch = false,
            backButtonVisible = isBackButtonVisible
        )
        notifyIfStateChanged(newState)
    }

    private fun detectFeedElements(rootNode: AccessibilityNodeInfo) {
        val reelsButton = elementDetector.findReelsButtonOptimized(rootNode)
        val isInFeed = true

        val newState = DetectionState(
            reelsVisible = reelsButton != null,
            inFeedTab = isInFeed,
            inSearchTab = false,
            inDirectMessages = false,
            keyboardActiveInSearch = false,
            backButtonVisible = isBackButtonVisible,
            reelsCoordinates = reelsButton?.let { getNodeBounds(it) }
        )

        notifyIfStateChanged(newState)
    }

    private fun detectSearchElements(rootNode: AccessibilityNodeInfo) {
        val isInSearch = true
        val keyboardActive = contextAnalyzer.detectKeyboardActiveInSearch(rootNode)

        val newState = DetectionState(
            reelsVisible = false,
            inFeedTab = false,
            inSearchTab = isInSearch,
            inDirectMessages = false,
            keyboardActiveInSearch = keyboardActive,
            backButtonVisible = isBackButtonVisible
        )

        notifyIfStateChanged(newState)
    }

    private fun detectGeneralElements(rootNode: AccessibilityNodeInfo) {
        val reelsButton = elementDetector.findReelsButtonOptimized(rootNode)
        val feedTab = elementDetector.findFeedTabFast(rootNode)
        val searchTab = elementDetector.findSearchTabFast(rootNode)

        val keyboardActive = if (searchTab?.isSelected == true) {
            contextAnalyzer.detectKeyboardActiveInSearch(rootNode)
        } else {
            false
        }

        val newState = DetectionState(
            reelsVisible = reelsButton != null,
            inFeedTab = feedTab?.isSelected == true,
            inSearchTab = searchTab?.isSelected == true,
            inDirectMessages = false,
            keyboardActiveInSearch = keyboardActive,
            backButtonVisible = isBackButtonVisible,
            reelsCoordinates = reelsButton?.let { getNodeBounds(it) }
        )

        notifyIfStateChanged(newState)
    }

    private fun notifyBackButtonDetected() {
        try {
            val intent = Intent("com.example.colander.BACK_BUTTON_DETECTED")
            intent.putExtra("back_button_visible", true)
            sendBroadcast(intent)
            Log.d(TAG, "📡 Broadcast enviado: Botón BACK detectado")
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando broadcast de botón back: ${e.message}")
        }
    }

    private data class DetectionState(
        val reelsVisible: Boolean,
        val inFeedTab: Boolean,
        val inSearchTab: Boolean,
        val inDirectMessages: Boolean,
        val keyboardActiveInSearch: Boolean,
        val backButtonVisible: Boolean,
        val reelsCoordinates: Rect? = null
    )

    private fun notifyIfStateChanged(newState: DetectionState) {
        val hasChanges = newState.reelsVisible != previousReelsState ||
                newState.inFeedTab != previousFeedState ||
                newState.inSearchTab != previousSearchState ||
                newState.inDirectMessages != previousDirectState ||
                newState.keyboardActiveInSearch != previousKeyboardState ||
                newState.backButtonVisible != previousBackButtonState

        if (hasChanges) {
            // Actualizar estado global
            isReelsButtonVisible = newState.reelsVisible
            reelsButtonCoordinates = newState.reelsCoordinates
            isInFeedTab = newState.inFeedTab
            isInSearchTab = newState.inSearchTab
            isInDirectMessages = newState.inDirectMessages
            isKeyboardActiveInSearch = newState.keyboardActiveInSearch
            isBackButtonVisible = newState.backButtonVisible

            // Actualizar estado previo
            previousReelsState = newState.reelsVisible
            previousFeedState = newState.inFeedTab
            previousSearchState = newState.inSearchTab
            previousDirectState = newState.inDirectMessages
            previousKeyboardState = newState.keyboardActiveInSearch
            previousBackButtonState = newState.backButtonVisible

            // Enviar notificación
            notifyOverlayService()

            Log.d(TAG, "📡 Estado cambió - Reels: ${newState.reelsVisible}, Feed: ${newState.inFeedTab}, Search: ${newState.inSearchTab}, Directos: ${newState.inDirectMessages}, Teclado: ${newState.keyboardActiveInSearch}, Back: ${newState.backButtonVisible}")
        }
    }

    private fun getNodeBounds(node: AccessibilityNodeInfo): Rect {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect
    }

    private fun notifyOverlayService() {
        try {
            val intent = Intent("com.example.colander.INSTAGRAM_ELEMENTS_UPDATE")

            // Información del botón de Reels
            intent.putExtra("reels_visible", isReelsButtonVisible)
            if (reelsButtonCoordinates != null) {
                intent.putExtra("reels_left", reelsButtonCoordinates!!.left)
                intent.putExtra("reels_top", reelsButtonCoordinates!!.top)
                intent.putExtra("reels_right", reelsButtonCoordinates!!.right)
                intent.putExtra("reels_bottom", reelsButtonCoordinates!!.bottom)
            }

            // Información de tabs y estado
            intent.putExtra("in_feed_tab", isInFeedTab)
            intent.putExtra("in_search_tab", isInSearchTab)
            intent.putExtra("keyboard_active_in_search", isKeyboardActiveInSearch)
            intent.putExtra("in_direct_messages", isInDirectMessages)
            intent.putExtra("back_button_visible", isBackButtonVisible)

            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando broadcast: ${e.message}")
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "⚠️ AccessibilityService interrumpido")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            instance = null
            isReelsButtonVisible = false
            reelsButtonCoordinates = null
            feedCoordinates = null
            isInFeedTab = false
            isInSearchTab = false
            isInDirectMessages = false
            isKeyboardActiveInSearch = false
            isBackButtonVisible = false

            handler.removeCallbacksAndMessages(null)

            Log.d(TAG, "💀 AccessibilityService destruido limpiamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error en onDestroy: ${e.message}")
        }
    }
}