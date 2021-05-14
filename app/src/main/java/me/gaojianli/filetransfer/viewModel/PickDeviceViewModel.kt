package me.gaojianli.filetransfer.viewModel

import android.net.wifi.p2p.WifiP2pDevice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import well_of_file.Well_of_file

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
            Well_of_file.send(path, 11451)
            callback()
        }
    }

    fun recevieFile(address:String,saveTo:String,callback:(String) -> Unit){
        viewModelScope.launch {
            val fileName = Well_of_file.receive(address, 11451, saveTo)
            callback(fileName)
        }
    }
}