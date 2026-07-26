package com.sivpn.cepat.vpn

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object VpnController {

    fun prepareAndStartVpn(
        activity: Activity,
        notificationPermissionLauncher: ActivityResultLauncher<String>,
        vpnPermissionLauncher: ActivityResultLauncher<Intent>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                continuePrepareAndStartVpn(activity, vpnPermissionLauncher)
            }
        } else {
            continuePrepareAndStartVpn(activity, vpnPermissionLauncher)
        }
    }

    fun continuePrepareAndStartVpn(
        activity: Activity,
        vpnPermissionLauncher: ActivityResultLauncher<Intent>
    ) {
        val intent = VpnService.prepare(activity)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            startVpnService(activity)
        }
    }

    fun startVpnService(context: Context) {
        android.util.Log.d("VpnController", "sebelum startForegroundService()")
        val intent = Intent(context, SiVpnService::class.java)
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopVpnService(context: Context) {
        android.util.Log.d("VpnController", "sebelum startService() untuk stop")
        val intent = Intent(context, SiVpnService::class.java).apply {
            action = com.sivpn.cepat.TProxyService.ACTION_DISCONNECT
        }
        context.startService(intent)
        LogManager.addLog("Disconnecting...")
    }
}
