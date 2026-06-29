package com.adb.tool.core

data class AppInfo(
    val packageName: String,
    val appName: String = "",
    val isSystem: Boolean = false,
    val isEnabled: Boolean = true,
    val versionName: String = "",
    val apkPath: String = ""
) {
    val displayName: String
        get() = appName.ifEmpty { packageName }
}
