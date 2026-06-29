package com.adb.tool.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adb.tool.R
import com.adb.tool.core.FileItem
import java.text.DecimalFormat

class FileListAdapter(
    private val fileList: List<FileItem>,
    private val onItemClick: (FileItem) -> Unit
) : RecyclerView.Adapter<FileListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        val tvFileName: TextView = view.findViewById(R.id.tvFileName)
        val tvFileInfo: TextView = view.findViewById(R.id.tvFileInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = fileList[position]
        holder.tvFileName.text = file.name
        
        if (file.isDirectory) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_myplaces)
            holder.tvFileInfo.text = file.date
        } else {
            holder.ivIcon.setImageResource(android.R.drawable.ic_menu_save)
            holder.tvFileInfo.text = "${formatFileSize(file.size)}  ${file.date}"
        }

        holder.itemView.setOnClickListener {
            onItemClick(file)
        }
    }

    override fun getItemCount() = fileList.size

    private fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceAtMost(units.size - 1)
        val value = size / Math.pow(1024.0, index.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[index]
    }
}
