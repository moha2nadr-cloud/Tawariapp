package com.tawari.emergency

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class TawariApp : Application() {

    companion object {
        const val CHANNEL_ID = "tawari_emergency"
        const val CHANNEL_NAME = "إشعارات الطوارئ"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات نظام الطوارئ"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
