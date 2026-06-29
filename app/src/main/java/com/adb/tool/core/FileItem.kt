package com.adb.tool.core

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0,
    val date: String = "",
    val permission: String = ""
)
