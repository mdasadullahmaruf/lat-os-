package com.mdasadullahmaruf.latos

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class AccessibilityExecutor : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitor events if needed
    }

    override fun onInterrupt() {
        // Handle interruption
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service is ready
    }

    /**
     * Find and click element by text
     */
    fun findAndClick(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        
        for (node in nodes) {
            if (node.isClickable) {
                return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            // Check parent if not clickable
            val parent = node.parent
            if (parent?.isClickable == true) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }
        return false
    }

    /**
     * Find EditText and set text
     */
    fun findAndType(text: String, input: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        
        for (node in nodes) {
            if (node.className?.contains("EditText") == true) {
                val args = android.os.Bundle()
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, input)
                return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            }
        }
        return false
    }

    /**
     * Tap at specific coordinates (using gesture)
     */
    fun tap(x: Float, y: Float) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        dispatchGesture(gesture, null, null)
    }

    /**
     * Swipe from one point to another
     */
    fun swipe(startX: Float, startY: Float, endX: Float, endY: Float) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
            .build()
        dispatchGesture(gesture, null, null)
    }

    /**
     * Scroll down
     */
    fun scrollDown(): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val scrollable = findScrollableNode(rootNode)
        return scrollable?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) ?: false
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findScrollableNode(child)
            if (result != null) return result
        }
        return null
    }
}
