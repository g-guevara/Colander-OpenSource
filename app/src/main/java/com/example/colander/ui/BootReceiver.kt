package com.example.colander

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.d(TAG, "📱 Device booted - checking if should restart service")
                restartServiceIfNeeded(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                Log.d(TAG, "📦 Package updated - checking if should restart service")
                restartServiceIfNeeded(context)
            }
        }
    }

    private fun restartServiceIfNeeded(context: Context) {
        try {
            // Verificar si el usuario había activado el servicio previamente
            val prefs = context.getSharedPreferences("cube_settings", Context.MODE_PRIVATE)
            val wasServiceActive = prefs.getBoolean("service_was_active", false)

            if (wasServiceActive) {
                Log.d(TAG, "🔄 Restarting AppDetectionService after boot/update")

                val serviceIntent = Intent(context, AppDetectionService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            } else {
                Log.d(TAG, "⏸️ Service was not active before, not restarting")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error restarting service: ${e.message}")
        }
    }
}