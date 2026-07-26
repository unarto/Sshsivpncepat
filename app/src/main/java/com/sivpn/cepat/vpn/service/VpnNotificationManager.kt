package com.sivpn.cepat.vpn.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.sivpn.cepat.ui.MainActivity
import com.sivpn.cepat.vpn.SiVpnService

object VpnNotificationManager {
    const val NOTIFICATION_ID = 1
    const val CHANNEL_ID = "vpn_channel"

    fun showForegroundNotification(service: Service) {
        createChannel(service)
        val notification = buildNotification(service, "Koneksi aman sedang berjalan")
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            service.startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            service.startForeground(NOTIFICATION_ID, notification)
        }
    }

    fun updateForegroundNotification(context: Context, text: String) {
        val notification = buildNotification(context, text)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VPN Status",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(context: Context, text: String): android.app.Notification {
        val activityIntent = Intent(context, MainActivity::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags = flags or PendingIntent.FLAG_IMMUTABLE
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, activityIntent, flags
        )

        val stopIntent = Intent(context, SiVpnService::class.java).apply {
            action = SiVpnService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 1, stopIntent, flags
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("SiVPN Aktif")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_delete, "MATIKAN", stopPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }
}
