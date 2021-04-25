package me.gaojianli.filetransfer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.internal.ContextUtils.getActivity
import me.gaojianli.filetransfer.ui.theme.Blue500
import me.gaojianli.filetransfer.ui.theme.FileTransferTheme
import me.gaojianli.filetransfer.wifi.WiFiDirectBroadcastReceiver

class MainActivity : ComponentActivity() {
    private val TAG = "WIFI_LOG"
    private val PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION = 1001
    private val permissionList = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Column {
                TopAppBar(
                    title = { Text("File Transfer") },
                    backgroundColor = Blue500,
                    contentColor = Color.White
                )
                MainView(
                    onSend = {
                        val intent = Intent(this@MainActivity,PickDeviceActivity::class.java)
                        startActivity(intent)
                    },
                    onReceive = {}
                )
            }
        }
        permissionList.map { applyPermission(it) }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE_ACCESS_FINE_LOCATION -> if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(
                    TAG,
                    "Fine location permission is not granted!"
                )
                finish()
            }
        }
    }

    private fun applyPermission(permission: String) {
        if (ActivityCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), 1);
        }
    }
}

@Composable
fun MainView(onSend: () -> Unit, onReceive: () -> Unit) {
    FileTransferTheme {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Welcome to the file transfer!", fontSize = 30.sp)
            Spacer(modifier = Modifier.height(100.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(IntrinsicSize.Max)) {
                    Button(
                        onClick = { onSend() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Text(text = "I want to send")
                    }
                    Spacer(modifier = Modifier.height(50.dp))
                    Button(
                        onClick = { onReceive() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Text(text = "I want to receive")
                    }
                }
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MainView(onSend = {}, onReceive = {})
}
