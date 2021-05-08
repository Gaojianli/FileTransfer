package me.gaojianli.filetransfer

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WpsInfo
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gaojianli.filetransfer.callback.DirectActionListener
import me.gaojianli.filetransfer.ui.theme.Blue500
import me.gaojianli.filetransfer.ui.theme.FileTransferTheme
import me.gaojianli.filetransfer.ui.theme.LoadingDialog
import me.gaojianli.filetransfer.wifi.WiFiDirectBroadcastReceiver
import java.util.*

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.net.wifi.p2p.*
import androidx.activity.result.contract.ActivityResultContract
import well_of_file.Well_of_file
import android.provider.MediaStore
import me.gaojianli.filetransfer.utils.UriUtils
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream


val TYPE_SEND = "send"
val TYPE_RECEIVE = "receive"

class PickDeviceActivity : ComponentActivity() {
    private val intentFilter = IntentFilter()
    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private var wifiP2pInfo: WifiP2pInfo? = null
    private lateinit var broadcastReceiver: BroadcastReceiver
    private var isSending: Boolean = false
    private var fileUri:Uri? = null
    private val activityLauncher = registerForActivityResult(ResultContract()) {
        it?.let {
            fileUri = it
            Toast.makeText(this, it.path, Toast.LENGTH_LONG).show()
            createGroup()
        }
    }
    private var connectionInfoAvailable = false
    private lateinit var mWifiP2pDevice: WifiP2pDevice
    private var wifiP2pEnabled = false
    private lateinit var peerList: ArrayList<WifiP2pDevice>
    private val viewModel = PickDeviceViewModel()
    private val TAG = "PickDeviceActivity"
    private var Type: String? = null
    private lateinit var dialog: LoadingDialog
    private val directActionListener: DirectActionListener = object : DirectActionListener {
        override fun wifiP2pEnabled(enabled: Boolean) {
            wifiP2pEnabled = enabled
        }

        override fun onConnectionInfoAvailable(wifiP2pInfo: WifiP2pInfo) {
            peerList.clear()
            val stringBuilder = StringBuilder()
            if (Type == TYPE_SEND) {
                stringBuilder.append("onConnectionInfoAvailable")
                stringBuilder.append("isGroupOwner：" + wifiP2pInfo.isGroupOwner)
                stringBuilder.append("groupFormed：" + wifiP2pInfo.groupFormed)
                if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
                    connectionInfoAvailable = true
                    Toast.makeText(this@PickDeviceActivity, stringBuilder.toString(), Toast.LENGTH_LONG)
                        .show()
                    if(!isSending){
                        isSending = true
                        dialog.setText("Serving")
                        val path = UriUtils.getPathFromUri(this@PickDeviceActivity,fileUri)
                        viewModel.serverFile(path) {
                            dialog.dismiss()
                            isSending = false
                        }
                    }
                }
            } else {

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
                Log.d(TAG, stringBuilder.toString())
                Toast.makeText(this@PickDeviceActivity, stringBuilder.toString(), Toast.LENGTH_LONG)
                    .show()
                if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner) {
                    this@PickDeviceActivity.wifiP2pInfo = wifiP2pInfo
                    dialog.show(hintText = "Start Receving", cancelable = true, canceledOnTouchOutside = false)
                    Well_of_file.receive(wifiP2pInfo.groupOwnerAddress.hostAddress,11451,"/data/local/tmp")
                    dialog.dismiss()
                }
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
            if (Type == TYPE_RECEIVE)
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
        Type = intent.getStringExtra("type")
        dialog = LoadingDialog(this)
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
        //搜寻附近带有 Wi-Fi P2P 的设备
        if (Type == TYPE_RECEIVE) {
            val PERMISSIONS_STORAGE = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (PackageManager.PERMISSION_GRANTED !=
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_CONTACTS
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    1
                )
            }
            dialog.show(hintText = "Searching", cancelable = true, canceledOnTouchOutside = false)
            viewModel.discovery { discovery() }
        } else {
            val PERMISSIONS_STORAGE = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (PackageManager.PERMISSION_GRANTED !=
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_CONTACTS
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    1
                )
            }
            dialog.show(hintText = "Waiting for connection", cancelable = true, canceledOnTouchOutside = false)
            activityLauncher.launch(false)
        }
    }

    private fun connect() {
        val config = WifiP2pConfig()
        if (config.deviceAddress != null) {
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
                    Log.d(TAG, "connect onSuccess")
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
        broadcastReceiver =
            WiFiDirectBroadcastReceiver(manager, channel, this, directActionListener)
        applicationContext.registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        applicationContext.unregisterReceiver(broadcastReceiver)
    }

    class ResultContract : ActivityResultContract<Boolean, Uri?>() {
        override fun createIntent(context: Context, input: Boolean?): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            if (resultCode == Activity.RESULT_OK) {//是否选择，没选择就不会继续
                return intent?.data;//得到uri，后面就是将uri转化成file的过程。
            }
            return null
        }
    }

    private fun createGroup() {
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
            return
        }
        removeGroup()
        manager.createGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(this@PickDeviceActivity, "Create group succeed", Toast.LENGTH_LONG).show()
                dialog.show("Waiting for connection",true,false)
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(this@PickDeviceActivity, "Create group failed", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                finish()
            }
        })
    }

    private fun removeGroup() {
        manager.removeGroup(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Toast.makeText(this@PickDeviceActivity, "onSuccess", Toast.LENGTH_LONG).show()
            }

            override fun onFailure(reason: Int) {
                Toast.makeText(this@PickDeviceActivity, "onFailure", Toast.LENGTH_LONG).show()
            }
        })
    }
}

class PickDeviceViewModel : ViewModel() {
    var peerList: List<WifiP2pDevice> by mutableStateOf(listOf())
    fun discovery(discoveryFun: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            discoveryFun()
        }
    }

    fun setPeerList(list: Collection<WifiP2pDevice>) {
        peerList = list.toList()
    }

    fun serverFile(path:String,callback:()->Unit){
        viewModelScope.launch(Dispatchers.IO) {
            Well_of_file.send(path,11451)
            callback()
        }
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