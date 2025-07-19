package com.example.colander

import android.content.Intent
import android.graphics.Rect
import android.util.Log

class ElementStateManager {

    companion object {
        private const val TAG = "ElementStateManager"
    }

    // Estados actuales de Instagram
    var isReelsButtonVisible = false
    var reelsButtonCoordinates: Rect? = null
    var isInFeedTab = false
    var isInSearchTab = false
    var isKeyboardActiveInSearch = false
    var isBackButtonVisible = false

    // Control de estado previo para evitar operaciones innecesarias
    private var previousFeedTabState = false
    private var previousSearchTabState = false
    private var previousReelsState = false
    private var previousKeyboardState = false
    private var previousBackButtonState = false

    /**
     * Actualiza el estado desde un Intent de broadcast
     * @return true si hubo cambios en el estado, false si no
     */
    fun updateFromIntent(intent: Intent): Boolean {
        // Información del botón de Reels
        val reelsVisible = intent.getBooleanExtra("reels_visible", false)

        if (reelsVisible) {
            val left = intent.getIntExtra("reels_left", 0)
            val top = intent.getIntExtra("reels_top", 0)
            val right = intent.getIntExtra("reels_right", 0)
            val bottom = intent.getIntExtra("reels_bottom", 0)

            reelsButtonCoordinates = Rect(left, top, right, bottom)
            isReelsButtonVisible = true
            Log.d(TAG, "Coordenadas Reels: $reelsButtonCoordinates")
        } else {
            isReelsButtonVisible = false
            reelsButtonCoordinates = null
        }

        // Estado del feed_tab
        val newFeedTabState = intent.getBooleanExtra("in_feed_tab", false)

        // Estado del search_tab
        val newSearchTabState = intent.getBooleanExtra("in_search_tab", false)

        // Estado del teclado en search
        val newKeyboardState = intent.getBooleanExtra("keyboard_active_in_search", false)

        // Estado del botón back
        val newBackButtonState = intent.getBooleanExtra("back_button_visible", false)

        // Verificar si hay cambios
        val hasChanges = newFeedTabState != previousFeedTabState ||
                newSearchTabState != previousSearchTabState ||
                newKeyboardState != previousKeyboardState ||
                isReelsButtonVisible != previousReelsState ||
                newBackButtonState != previousBackButtonState

        if (hasChanges) {
            // Actualizar estados actuales
            isInFeedTab = newFeedTabState
            isInSearchTab = newSearchTabState
            isKeyboardActiveInSearch = newKeyboardState
            isBackButtonVisible = newBackButtonState

            // Actualizar estados previos
            previousFeedTabState = newFeedTabState
            previousSearchTabState = newSearchTabState
            previousKeyboardState = newKeyboardState
            previousReelsState = isReelsButtonVisible
            previousBackButtonState = newBackButtonState

            Log.d(TAG, "Estados actualizados - Feed: $isInFeedTab, Search: $isInSearchTab, Teclado: $isKeyboardActiveInSearch, Reels: $isReelsButtonVisible, Back: $isBackButtonVisible")
        }

        return hasChanges
    }

    /**
     * Limpia todos los estados (usado cuando Instagram se cierra)
     */
    fun clearState() {
        isReelsButtonVisible = false
        reelsButtonCoordinates = null
        isInFeedTab = false
        isInSearchTab = false
        isKeyboardActiveInSearch = false
        isBackButtonVisible = false

        previousFeedTabState = false
        previousSearchTabState = false
        previousKeyboardState = false
        previousReelsState = false
        previousBackButtonState = false

        Log.d(TAG, "Estados limpiados")
    }

    /**
     * Obtiene un resumen del estado actual para debugging
     */
    fun getStateSummary(): String {
        return "Feed: $isInFeedTab, Search: $isInSearchTab, Teclado: $isKeyboardActiveInSearch, " +
                "Reels: $isReelsButtonVisible, Back: $isBackButtonVisible, " +
                "ReelsCoords: $reelsButtonCoordinates"
    }

    /**
     * Verifica si cualquier overlay debería estar activo
     */
    fun shouldShowAnyOverlay(): Boolean {
        return isReelsButtonVisible ||
                (isInFeedTab && !isBackButtonVisible) ||
                (isInSearchTab && !isBackButtonVisible && !isKeyboardActiveInSearch)
    }

    /**
     * Verifica si los overlays de navegación (Feed/Search) deberían estar activos
     */
    fun shouldShowNavTabOverlays(): Boolean {
        return !isBackButtonVisible &&
                ((isInFeedTab) || (isInSearchTab && !isKeyboardActiveInSearch))
    }
}