package com.adb.tool.utils

import android.content.Context
import android.content.SharedPreferences

class PrefsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("adb_tool_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_IP = "last_ip"
        private const val KEY_LAST_PORT = "last_port"
        private const val KEY_COMMAND_HISTORY = "command_history"
        private const val KEY_AUTO_CONNECT = "auto_connect"
    }

    var lastIp: String
        get() = prefs.getString(KEY_LAST_IP, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_IP, value).apply()

    var lastPort: Int
        get() = prefs.getInt(KEY_LAST_PORT, 5555)
        set(value) = prefs.edit().putInt(KEY_LAST_PORT, value).apply()

    var autoConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CONNECT, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CONNECT, value).apply()

    fun addCommandHistory(command: String) {
        val history = getCommandHistory().toMutableList()
        history.remove(command)
        history.add(0, command)
        if (history.size > 20) {
            history.removeAt(history.size - 1)
        }
        prefs.edit().putStringSet(KEY_COMMAND_HISTORY, history.toSet()).apply()
    }

    fun getCommandHistory(): List<String> {
        return prefs.getStringSet(KEY_COMMAND_HISTORY, emptySet())?.toList() ?: emptyList()
    }

    fun clearCommandHistory() {
        prefs.edit().remove(KEY_COMMAND_HISTORY).apply()
    }
}
