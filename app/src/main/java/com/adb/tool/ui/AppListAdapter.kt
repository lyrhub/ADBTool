package com.adb.tool.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.adb.tool.R
import com.adb.tool.core.AppInfo

class AppListAdapter(
    private val appList: List<AppInfo>,
    private val onAction: (AppInfo, String) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    private val selectedApps = mutableSetOf<AppInfo>()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAppName: TextView = view.findViewById(R.id.tvAppName)
        val tvPackageName: TextView = view.findViewById(R.id.tvPackageName)
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val btnDisable: ImageButton = view.findViewById(R.id.btnDisable)
        val btnUninstall: ImageButton = view.findViewById(R.id.btnUninstall)
        val btnClear: ImageButton = view.findViewById(R.id.btnClear)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = appList[position]
        holder.tvAppName.text = app.displayName
        holder.tvPackageName.text = app.packageName
        holder.checkBox.isChecked = selectedApps.contains(app)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedApps.add(app)
            } else {
                selectedApps.remove(app)
            }
        }

        holder.btnDisable.setOnClickListener {
            onAction(app, if (app.isEnabled) "disable" else "enable")
        }

        holder.btnUninstall.setOnClickListener {
            onAction(app, "uninstall")
        }

        holder.btnClear.setOnClickListener {
            onAction(app, "clear_data")
        }
    }

    override fun getItemCount() = appList.size

    fun getSelectedApps(): List<AppInfo> = selectedApps.toList()
}
