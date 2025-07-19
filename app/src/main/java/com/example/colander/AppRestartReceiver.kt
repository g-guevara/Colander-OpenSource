package com.example.colander

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class AppRestartReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "AppRestartReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.colander.RESTART_SERVICE") {
            Log.d(TAG, "🔄 Received restart signal")
            restartService(context)
        }
    }

    private fun restartService(context: Context) {
        try {
            val serviceIntent = Intent(context, AppDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            Log.d(TAG, "✅ Service restarted successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error restarting service: ${e.message}")
        }
    }
}