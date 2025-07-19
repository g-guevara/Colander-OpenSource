package com.example.colander

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

enum class InstagramContext {
    DIRECT_MESSAGES, FEED, SEARCH, OTHER
}

class InstagramContextAnalyzer {

    companion object {
        private const val TAG = "ContextAnalyzer"
        private const val maxSearchDepth = 8
    }

    private val elementDetector = InstagramElementDetector()

    fun detectInstagramContext(rootNode: AccessibilityNodeInfo): InstagramContext {
        // Detectar directos por elementos específicos
        val directIndicators = listOf(
            "direct_inbox",
            "DirectThreadFragment",
            "direct_thread",
            "message_list"
        )

        if (elementDetector.searchForAnyIndicator(rootNode, directIndicators, maxDepth = 4)) {
            Log.d(TAG, "📩 Contexto: DIRECTOS detectado")
            return InstagramContext.DIRECT_MESSAGES
        }

        // Detectar search tab por search_tab seleccionado
        val searchTab = elementDetector.findSearchTabFast(rootNode)
        if (searchTab != null && searchTab.isSelected) {
            Log.d(TAG, "🔍 Contexto: SEARCH detectado")
            return InstagramContext.SEARCH
        }

        // Detectar feed por feed_tab seleccionado
        val feedTab = elementDetector.findFeedTabFast(rootNode)
        if (feedTab != null && feedTab.isSelected) {
            Log.d(TAG, "🏠 Contexto: FEED detectado")
            return InstagramContext.FEED
        }

        Log.d(TAG, "❓ Contexto: OTROS")
        return InstagramContext.OTHER
    }

    fun detectKeyboardActiveInSearch(rootNode: AccessibilityNodeInfo): Boolean {
        try {
            // Buscar campos de texto activos/focusados
            val activeEditTexts = findActiveEditTexts(rootNode)

            // Buscar específicamente el campo de búsqueda de Instagram
            val searchField = findSearchField(rootNode)

            val hasActiveSearch = activeEditTexts.isNotEmpty() ||
                    (searchField != null && searchField.isFocused)

            if (hasActiveSearch) {
                Log.d(TAG, "⌨️ Teclado activo detectado en Search - Campos activos: ${activeEditTexts.size}")
            }

            return hasActiveSearch
        } catch (e: Exception) {
            Log.e(TAG, "Error detectando teclado: ${e.message}")
            return false
        }
    }

    private fun findActiveEditTexts(node: AccessibilityNodeInfo, depth: Int = 0): List<AccessibilityNodeInfo> {
        if (depth > maxSearchDepth) return emptyList()

        val activeFields = mutableListOf<AccessibilityNodeInfo>()

        // Verificar si el nodo actual es un campo de texto activo
        if (isActiveEditText(node)) {
            activeFields.add(node)
        }

        // Buscar en hijos
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            activeFields.addAll(findActiveEditTexts(child, depth + 1))
        }

        return activeFields
    }

    private fun findSearchField(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // IDs comunes del campo de búsqueda en Instagram
        val searchFieldIds = listOf(
            "com.instagram.android:id/action_bar_search_edit_text",
            "com.instagram.android:id/search_edit_text",
            "com.instagram.android:id/search_box",
            "com.instagram.android:id/search_field"
        )

        for (fieldId in searchFieldIds) {
            val fields = rootNode.findAccessibilityNodeInfosByViewId(fieldId)
            for (field in fields) {
                if (isValidSearchField(field)) {
                    return field
                }
            }
        }

        return null
    }

    private fun isActiveEditText(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString()

        return (className?.contains("EditText") == true ||
                className?.contains("TextInputEditText") == true) &&
                node.isVisibleToUser &&
                node.isEnabled &&
                (node.isFocused || node.isAccessibilityFocused) &&
                hasReasonableSize(node)
    }

    private fun isValidSearchField(node: AccessibilityNodeInfo): Boolean {
        return node.isVisibleToUser &&
                node.isEnabled &&
                hasReasonableSize(node) &&
                (node.isFocused || node.text?.isNotEmpty() == true)
    }

    private fun hasReasonableSize(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.width() > 20 && rect.height() > 20
    }
}