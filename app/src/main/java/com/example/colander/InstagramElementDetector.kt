package com.example.colander

import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

class InstagramElementDetector {

    companion object {
        private const val TAG = "ElementDetector"
        private const val maxSearchDepth = 8
    }

    // ========== BÚSQUEDA DE BOTÓN BACK ==========

    fun findBackButtonOptimized(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // Buscar por ID específico primero
        val backButtonNodes = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/action_bar_button_back")

        for (node in backButtonNodes) {
            if (isValidBackButton(node)) {
                Log.d(TAG, "🔙 action_bar_button_back encontrado por ID")
                return node
            }
        }

        // Si no se encuentra por ID, buscar en profundidad limitada
        return searchForBackButtonLimited(rootNode, 0)
    }

    private fun searchForBackButtonLimited(node: AccessibilityNodeInfo, currentDepth: Int): AccessibilityNodeInfo? {
        if (currentDepth >= maxSearchDepth) return null

        if (isBackButtonNode(node) && isValidBackButton(node)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchForBackButtonLimited(child, currentDepth + 1)
            if (result != null) return result
        }

        return null
    }

    private fun isBackButtonNode(node: AccessibilityNodeInfo): Boolean {
        val viewIdName = node.viewIdResourceName
        if (viewIdName?.contains("action_bar_button_back") == true) {
            return true
        }

        val className = node.className?.toString()
        if (className?.contains("ImageView") == true) {
            val contentDesc = node.contentDescription?.toString()?.lowercase()
            val backKeywords = listOf("back", "atrás", "volver", "navigate up", "up button")
            return backKeywords.any { keyword -> contentDesc?.contains(keyword) == true }
        }

        return false
    }

    private fun isValidBackButton(node: AccessibilityNodeInfo): Boolean {
        return node.isVisibleToUser &&
                node.isEnabled &&
                node.isClickable &&
                hasReasonableSize(node)
    }

    // ========== BÚSQUEDA DE TABS ==========

    fun findFeedTabFast(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val feedTabNodes = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/feed_tab")

        for (node in feedTabNodes) {
            if (isValidTab(node)) {
                return node
            }
        }

        return searchForFeedTabLimited(rootNode, 0)
    }

    fun findSearchTabFast(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val searchTabNodes = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/search_tab")

        for (node in searchTabNodes) {
            if (isValidTab(node)) {
                return node
            }
        }

        return searchForSearchTabLimited(rootNode, 0)
    }

    private fun searchForFeedTabLimited(node: AccessibilityNodeInfo, currentDepth: Int): AccessibilityNodeInfo? {
        if (currentDepth >= maxSearchDepth) return null

        if (isFeedTabNode(node) && isValidTab(node)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchForFeedTabLimited(child, currentDepth + 1)
            if (result != null) return result
        }

        return null
    }

    private fun searchForSearchTabLimited(node: AccessibilityNodeInfo, currentDepth: Int): AccessibilityNodeInfo? {
        if (currentDepth >= maxSearchDepth) return null

        if (isSearchTabNode(node) && isValidTab(node)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchForSearchTabLimited(child, currentDepth + 1)
            if (result != null) return result
        }

        return null
    }

    // ========== BÚSQUEDA DE REELS BUTTON ==========

    fun findReelsButtonOptimized(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val clipsTabNodes = rootNode.findAccessibilityNodeInfosByViewId("com.instagram.android:id/clips_tab")

        for (node in clipsTabNodes) {
            if (isValidReelsButton(node)) {
                return node
            }
        }

        return searchForClipsTabLimited(rootNode, 0)
    }

    private fun searchForClipsTabLimited(node: AccessibilityNodeInfo, currentDepth: Int): AccessibilityNodeInfo? {
        if (currentDepth >= maxSearchDepth) return null

        if (isClipsTabNode(node) && isValidReelsButton(node)) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchForClipsTabLimited(child, currentDepth + 1)
            if (result != null) return result
        }

        return null
    }

    // ========== VALIDACIONES DE NODOS ==========

    private fun isValidTab(node: AccessibilityNodeInfo): Boolean {
        return node.isVisibleToUser &&
                node.isEnabled &&
                hasReasonableSize(node)
    }

    private fun isValidReelsButton(node: AccessibilityNodeInfo): Boolean {
        return node.isVisibleToUser &&
                node.isEnabled &&
                node.isFocusable &&
                node.isClickable &&
                hasReasonableSize(node)
    }

    private fun isFeedTabNode(node: AccessibilityNodeInfo): Boolean {
        val viewIdName = node.viewIdResourceName
        if (viewIdName?.contains("feed_tab") == true) {
            return true
        }

        val className = node.className?.toString()
        if (className?.contains("FrameLayout") == true) {
            val contentDesc = node.contentDescription?.toString()?.lowercase()
            val feedKeywords = listOf("home", "feed", "inicio")
            return feedKeywords.any { keyword -> contentDesc?.contains(keyword) == true }
        }

        return false
    }

    private fun isSearchTabNode(node: AccessibilityNodeInfo): Boolean {
        val viewIdName = node.viewIdResourceName
        if (viewIdName?.contains("search_tab") == true) {
            return true
        }

        val className = node.className?.toString()
        if (className?.contains("FrameLayout") == true) {
            val contentDesc = node.contentDescription?.toString()?.lowercase()
            val searchKeywords = listOf("search", "buscar", "busqueda", "explore", "explorar")
            return searchKeywords.any { keyword -> contentDesc?.contains(keyword) == true }
        }

        return false
    }

    private fun isClipsTabNode(node: AccessibilityNodeInfo): Boolean {
        val viewIdName = node.viewIdResourceName
        if (viewIdName?.contains("clips_tab") == true) {
            return true
        }

        val className = node.className?.toString()
        if (className?.contains("FrameLayout") == true) {
            val contentDesc = node.contentDescription?.toString()?.lowercase()
            val reelsKeywords = listOf("reels", "reel", "clips")
            return reelsKeywords.any { keyword -> contentDesc?.contains(keyword) == true }
        }

        return false
    }

    private fun hasReasonableSize(node: AccessibilityNodeInfo): Boolean {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return rect.width() > 20 && rect.height() > 20
    }

    // ========== BÚSQUEDA GENÉRICA ==========

    fun searchForAnyIndicator(node: AccessibilityNodeInfo, indicators: List<String>, maxDepth: Int, currentDepth: Int = 0): Boolean {
        if (currentDepth >= maxDepth) return false

        val viewId = node.viewIdResourceName
        val className = node.className?.toString()
        val contentDesc = node.contentDescription?.toString()

        for (indicator in indicators) {
            if (viewId?.contains(indicator, ignoreCase = true) == true ||
                className?.contains(indicator, ignoreCase = true) == true ||
                contentDesc?.contains(indicator, ignoreCase = true) == true) {
                return true
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (searchForAnyIndicator(child, indicators, maxDepth, currentDepth + 1)) {
                return true
            }
        }

        return false
    }
}