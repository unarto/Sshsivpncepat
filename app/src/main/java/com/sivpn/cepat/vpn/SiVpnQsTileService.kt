package com.sivpn.cepat.vpn

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.sivpn.cepat.ui.MainActivity

class SiVpnQsTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()

        val isRunning = SiVpnService.isRunning

        if (isRunning) {
            // Stop tunnel
            val intent = Intent(this, SiVpnService::class.java).apply {
                action = SiVpnService.ACTION_STOP
            }
            startService(intent)
            updateTile(false)
            return
        }

        // Start tunnel
        val prepare = VpnService.prepare(this)

        if (prepare != null) {
            // VPN permission hasn't been granted yet
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
            return
        }

        // Permission granted, start VPN
        val intent = Intent(this, SiVpnService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        updateTile(true)
    }

    private fun updateTile(forceState: Boolean? = null) {
        val tile = qsTile ?: return
        
        val isRunning = forceState ?: SiVpnService.isRunning

        tile.state = if (isRunning) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }

        tile.label = "SiVPN Cepat"
        tile.updateTile()
    }

    companion object {
        fun requestUpdate(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                TileService.requestListeningState(
                    context,
                    ComponentName(context, SiVpnQsTileService::class.java)
                )
            }
        }
    }
}
