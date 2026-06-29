package com.adb.tool.utils

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

class NetworkUtils {
    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (netInterface in interfaces) {
                    val addresses = Collections.list(netInterface.inetAddresses)
                    for (addr in addresses) {
                        if (!addr.isLoopbackAddress) {
                            val sAddr = addr.hostAddress
                            if (sAddr != null && sAddr.indexOf(':') < 0) {
                                return sAddr
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "192.168.1.1"
        }

        fun getSubnet(): String {
            val ip = getLocalIpAddress()
            return ip.substring(0, ip.lastIndexOf('.'))
        }

        fun scanNetwork(subnet: String, onDeviceFound: (String) -> Unit): List<String> {
            val devices = mutableListOf<String>()
            
            for (i in 1..254) {
                val ip = "$subnet.$i"
                try {
                    val address = InetAddress.getByName(ip)
                    if (address.isReachable(500)) {
                        devices.add(ip)
                        onDeviceFound(ip)
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
            
            return devices.sorted()
        }

        fun isWifiConnected(context: Context): Boolean {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            return wifiManager.isWifiEnabled && wifiManager.connectionInfo != null
        }
    }
}
