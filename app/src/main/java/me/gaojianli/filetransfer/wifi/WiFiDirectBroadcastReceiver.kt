package me.gaojianli.filetransfer.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import me.gaojianli.filetransfer.MainActivity
import me.gaojianli.filetransfer.PickDeviceActivity
import me.gaojianli.filetransfer.callback.DirectActionListener
import java.util.ArrayList

class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    private val activity: Context,
    private var mDirectActionListener: DirectActionListener
) : BroadcastReceiver() {
    private val TAG: String = "DirectBroadcastReceiver"
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    // Determine if Wifi P2P mode is enabled or not, alert
                    // the Activity.
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        mDirectActionListener.wifiP2pEnabled(false)
                        val wifiP2pDeviceList: List<WifiP2pDevice> = ArrayList()
                        mDirectActionListener.onPeersAvailable(wifiP2pDeviceList)
                    }else{
                        mDirectActionListener.wifiP2pEnabled(true)
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    if (ActivityCompat.checkSelfPermission(
                            activity,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        Toast.makeText(activity, "Missing permission!", Toast.LENGTH_LONG).show()
                        return
                    }
                    manager.requestPeers(channel) { peers ->
                        mDirectActionListener.onPeersAvailable(
                            peers.deviceList
                        )
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo =
                        intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    if (networkInfo != null && networkInfo.isConnected) {
                        manager.requestConnectionInfo(channel) { info ->
                            mDirectActionListener.onConnectionInfoAvailable(
                                info
                            )
                        }
                        Log.e(
                            TAG,
                            "已连接p2p设备"
                        )
                    } else {
                        mDirectActionListener.onDisconnection()
                        Log.e(
                           TAG,
                            "与p2p设备已断开连接"
                        )
                    }
                }
                WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                    val wifiP2pDevice =
                        intent.getParcelableExtra<WifiP2pDevice>(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE)
                    mDirectActionListener.onSelfDeviceAvailable(wifiP2pDevice!!)
                }
            }
        }
    }
    companion object{
        var connected:Boolean = false
    }
}