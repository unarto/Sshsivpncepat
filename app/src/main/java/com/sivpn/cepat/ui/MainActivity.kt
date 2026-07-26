package com.sivpn.cepat.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.sivpn.cepat.ui.MainScreen
import com.sivpn.cepat.ui.theme.MyApplicationTheme
import com.sivpn.cepat.vpn.JniLibHelper
import com.sivpn.cepat.vpn.LogManager
import com.sivpn.cepat.vpn.VpnController
import com.sivpn.cepat.viewmodel.MainViewModel
import com.sivpn.cepat.viewmodel.MainViewModelFactory
import com.sivpn.cepat.viewmodel.DialogViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(this)
    }


    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            LogManager.addLog("[Notifikasi] Izin notifikasi diberikan")
        } else {
            LogManager.addLog("[Notifikasi] Izin notifikasi ditolak")
            Toast.makeText(this, "Izin notifikasi ditolak. Anda tidak akan melihat notifikasi status VPN.", Toast.LENGTH_SHORT).show()
        }
        VpnController.continuePrepareAndStartVpn(this, vpnPermissionLauncher)
    }

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            VpnController.startVpnService(this)
        } else {
            LogManager.addLog("VPN Permission denied")
            Toast.makeText(this, "Permisi VPN ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        JniLibHelper.loadDownloadedLibs(this)
        enableEdgeToEdge()

        setContent {
            val uiState = viewModel.uiState.value
            val darkTheme = when (uiState.themeMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                MainScreen(
                    viewModel = viewModel,
                    dialogViewModel = viewModel(),
                    onConnect = {
                        VpnController.prepareAndStartVpn(
                            activity = this@MainActivity,
                            notificationPermissionLauncher = notificationPermissionLauncher,
                            vpnPermissionLauncher = vpnPermissionLauncher
                        )
                    },
                    onDisconnect = {
                        VpnController.stopVpnService(this@MainActivity)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startSpeedMonitor(this)
    }
}
