package me.gaojianli.filetransfer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Card
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.compose.runtime.setValue
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gaojianli.filetransfer.callback.DirectActionListener
import me.gaojianli.filetransfer.ui.theme.Blue500
import me.gaojianli.filetransfer.ui.theme.FileTransferTheme
import me.gaojianli.filetransfer.ui.theme.LoadingDialog
import me.gaojianli.filetransfer.wifi.WiFiDirectBroadcastReceiver
import java.lang.StringBuilder
import java.util.ArrayList

class PickDeviceActivity : ComponentActivity() {
    private val intentFilter = IntentFilter()
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private var wifiP2pInfo: WifiP2pInfo? = null
    private lateinit var broadcastReceiver: BroadcastReceiver

    private lateinit var mWifiP2pDevice: WifiP2pDevice
    private var wifiP2pEnabled = false
    private lateinit var peerList: ArrayList<WifiP2pDevice>
    private val viewModel = PickDeviceViewModel()
    private val TAG = "PickDeviceActivity"
    private val dialog = LoadingDialog(this)
    private val directActionListener: DirectActionListener = object : DirectActionListener {
        override fun wifiP2pEnabled(enabled: Boolean) {
            wifiP2pEnabled = enabled
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            peerList.clear()
            val stringBuilder = StringBuilder()
            stringBuilder.append("连接的设备名：")
            stringBuilder.append(mWifiP2pDevice.deviceName)
            stringBuilder.append("\n")
            stringBuilder.append("连接的设备的地址：")
            stringBuilder.append(mWifiP2pDevice.deviceAddress)
            stringBuilder.append("\n")
            stringBuilder.append("是否群主：")
            stringBuilder.append(if (wifiP2pInfo.isGroupOwner) "是群主" else "非群主")
            stringBuilder.append("\n")
            stringBuilder.append("群主IP地址：")
            stringBuilder.append(wifiP2pInfo.groupOwnerAddress.hostAddress)
            Log.d(TAG,stringBuilder.toString()
            )
            if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
                this@PickDeviceActivity.wifiP2pInfo = wifiP2pInfo
            }
        }

        override fun onDisconnection() {
            peerList.clear()
            this@PickDeviceActivity.wifiP2pInfo = null
        }

        override fun onSelfDeviceAvailable(wifiP2pDevice: WifiP2pDevice) {

        }

        override fun onPeersAvailable(wifiP2pDeviceList: Collection<WifiP2pDevice>) {
            this@PickDeviceActivity.peerList.addAll(wifiP2pDeviceList)
            this@PickDeviceActivity.viewModel.setPeerList(wifiP2pDeviceList)
            dialog.cancel()
        }

        override fun onChannelDisconnected() {
            Log.e(TAG, "onChannelDisconnected")
        }
    }

    private fun discovery() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(this@PickDeviceActivity, "Search success!", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(this@PickDeviceActivity, "Search failed!", Toast.LENGTH_SHORT).show()

            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        manager = getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
        peerList = ArrayList()
        channel = manager.initialize(this, mainLooper, null)
        broadcastReceiver =
            WiFiDirectBroadcastReceiver(manager, channel, this, directActionListener)
        registerReceiver(broadcastReceiver, intentFilter)
        setContent {
            Column {
                TopAppBar(
                    title = { Text("Device List") },
                    backgroundColor = Blue500,
                    contentColor = Color.White
                )
                DeviceListView(viewModel, onConnect = {
                    mWifiP2pDevice = it
                    connect()
                })
            }
        }
        dialog.show(hintText = "Searching",cancelable = true,canceledOnTouchOutside = false)
        //搜寻附近带有 Wi-Fi P2P 的设备
        viewModel.discovery { discovery() }
    }

    private fun connect() {
        val config = WifiP2pConfig()
        if (config.deviceAddress != null && mWifiP2pDevice != null) {
            config.deviceAddress = mWifiP2pDevice.deviceAddress
            config.wps.setup = WpsInfo.PBC
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    1
                )
            }
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.e(TAG, "connect onSuccess")
                }

                override fun onFailure(reason: Int) {
                    Toast.makeText(this@PickDeviceActivity, "连接失败 $reason", Toast.LENGTH_SHORT)
                        .show()
                }
            })
        }
    }

    public override fun onResume() {
        super.onResume()
        broadcastReceiver = WiFiDirectBroadcastReceiver(manager, channel, this,directActionListener)
        applicationContext.registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        applicationContext.unregisterReceiver(broadcastReceiver)
    }


}

class PickDeviceViewModel : ViewModel() {
    var peerList:List<WifiP2pDevice> by mutableStateOf(listOf())
    fun discovery(discoveryFun: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            discoveryFun()
        }
    }
    fun setPeerList(list: Collection<WifiP2pDevice>){
        peerList = list.toList()
    }
}

@Preview
@Composable
fun PreviewDeviceList() {
    val device = WifiP2pDevice()
    val device2 = WifiP2pDevice()
    device.deviceAddress = "ac:de:48:00:11:22"
    device.deviceName = "Test Device"
    device2.deviceAddress = "ab:cd:ef:00:11:22"
    device2.deviceName = "Test Device2"
    DeviceList(peerList = arrayListOf(device, device2), onConnect = {})
}

@Composable
fun DeviceTiles(device: WifiP2pDevice, onClick: (WifiP2pDevice) -> Unit) {
    Card(elevation = 4.dp, modifier = Modifier.clickable(onClick = { onClick(device) })) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(text = device.deviceName, fontSize = 20.sp)
            Text(text = device.deviceAddress, fontSize = 10.sp, color = Color.Gray)
        }
    }
}


@Composable
fun DeviceList(peerList: List<WifiP2pDevice>, onConnect: (WifiP2pDevice) -> Unit) {
    FileTransferTheme {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(peerList.size) {
                Box(modifier = Modifier.padding(5.dp)) {
                    DeviceTiles(device = peerList[it], onClick = onConnect)
                }
            }
        }
    }
}

@Composable
fun DeviceListView(viewModel: PickDeviceViewModel, onConnect: (WifiP2pDevice) -> Unit) {
    if (viewModel.peerList.isEmpty())
        Text(text = "Searching...")
    else
        DeviceList(peerList = viewModel.peerList, onConnect)
}