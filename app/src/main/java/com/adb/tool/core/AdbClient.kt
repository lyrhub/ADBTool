package com.adb.tool.core

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AdbClient(private val host: String, private val port: Int = 5555) {
    private var socket: Socket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    var isConnected = false
        private set

    @Synchronized
    fun connect(): Boolean {
        return try {
            socket = Socket(host, port)
            socket?.soTimeout = 10000
            outputStream = socket?.getOutputStream()
            inputStream = socket?.getInputStream()
            isConnected = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            isConnected = false
            false
        }
    }

    @Synchronized
    fun disconnect() {
        try {
            socket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isConnected = false
        socket = null
        outputStream = null
        inputStream = null
    }

    private fun writeMessage(message: String) {
        val data = message.toByteArray(Charsets.UTF_8)
        val length = String.format("%04x", data.size).toByteArray(Charsets.UTF_8)
        outputStream?.write(length)
        outputStream?.write(data)
        outputStream?.flush()
    }

    private fun readResponse(): String {
        val lengthBytes = ByteArray(4)
        var read = inputStream?.read(lengthBytes) ?: 0
        if (read != 4) throw Exception("Failed to read length")
        
        val lengthStr = String(lengthBytes, Charsets.UTF_8)
        val length = lengthStr.toIntOrNull(16) ?: 0
        
        val data = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val chunk = inputStream?.read(data, totalRead, length - totalRead) ?: -1
            if (chunk == -1) break
            totalRead += chunk
        }
        return String(data, 0, totalRead, Charsets.UTF_8)
    }

    private fun readStatus(): String {
        val statusBytes = ByteArray(4)
        inputStream?.read(statusBytes)
        return String(statusBytes, Charsets.UTF_8)
    }

    fun executeCommand(command: String): String {
        if (!isConnected) throw Exception("Not connected")
        return try {
            writeMessage("shell:$command")
            val status = readStatus()
            if (status == "OKAY") {
                readAllData()
            } else {
                "Error: $status"
            }
        } catch (e: Exception) {
            "Exception: ${e.message}"
        }
    }

    private fun readAllData(): String {
        val result = StringBuilder()
        val buffer = ByteArray(4096)
        try {
            socket?.soTimeout = 500
            while (true) {
                val len = inputStream?.read(buffer) ?: -1
                if (len <= 0) break
                result.append(String(buffer, 0, len, Charsets.UTF_8))
            }
        } catch (e: Exception) {
            // timeout is expected
        } finally {
            socket?.soTimeout = 10000
        }
        return result.toString()
    }

    // 应用管理相关命令
    fun listPackages(includeSystem: Boolean = true): List<String> {
        val cmd = if (includeSystem) "pm list packages" else "pm list packages -3"
        val result = executeCommand(cmd)
        return result.lines()
            .map { it.removePrefix("package:").trim() }
            .filter { it.isNotEmpty() }
            .sorted()
    }

    fun disableApp(packageName: String): String {
        return executeCommand("pm disable-user --user 0 $packageName")
    }

    fun enableApp(packageName: String): String {
        return executeCommand("pm enable $packageName")
    }

    fun uninstallApp(packageName: String): String {
        return executeCommand("pm uninstall --user 0 $packageName")
    }

    fun installApp(apkPath: String): String {
        return executeCommand("pm install -r $apkPath")
    }

    fun clearAppData(packageName: String): String {
        return executeCommand("pm clear $packageName")
    }

    fun getAppVersion(packageName: String): String {
        return executeCommand("dumpsys package $packageName | grep versionName")
    }

    // 文件管理相关命令
    fun listFiles(path: String): String {
        return executeCommand("ls -la $path")
    }

    fun pushFile(localPath: String, remotePath: String): String {
        return executeCommand("push $localPath $remotePath")
    }

    fun pullFile(remotePath: String, localPath: String): String {
        return executeCommand("pull $remotePath $localPath")
    }

    // 系统相关命令
    fun reboot(): String {
        return executeCommand("reboot")
    }

    fun rebootRecovery(): String {
        return executeCommand("reboot recovery")
    }

    fun rebootBootloader(): String {
        return executeCommand("reboot bootloader")
    }

    fun takeScreenshot(savePath: String): String {
        return executeCommand("screencap -p $savePath")
    }

    fun getDeviceInfo(): String {
        return buildString {
            appendLine("=== 设备信息 ===")
            appendLine(executeCommand("getprop ro.product.model"))
            appendLine(executeCommand("getprop ro.build.version.release"))
            appendLine(executeCommand("getprop ro.build.version.sdk"))
            appendLine(executeCommand("getprop ro.product.brand"))
        }
    }

    fun getRunningServices(): String {
        return executeCommand("dumpsys activity services")
    }

    fun getBatteryInfo(): String {
        return executeCommand("dumpsys battery")
    }
}
