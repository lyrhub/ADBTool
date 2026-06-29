package com.adb.tool.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.adb.tool.R
import com.adb.tool.core.AdbClient
import com.adb.tool.core.AppInfo
import com.adb.tool.databinding.FragmentAppsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppsFragment : Fragment(), MainActivity.OnAdbConnectionListener {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!
    private var adbClient: AdbClient? = null
    private val appList = mutableListOf<AppInfo>()
    private var appAdapter: AppListAdapter? = null
    private var showSystemApps = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        appAdapter = AppListAdapter(appList) { appInfo, action ->
            handleAppAction(appInfo, action)
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appAdapter
        }

        binding.btnRefresh.setOnClickListener {
            loadApps()
        }

        binding.switchSystemApps.setOnCheckedChangeListener { _, isChecked ->
            showSystemApps = isChecked
            loadApps()
        }

        binding.btnUninstallSelected.setOnClickListener {
            uninstallSelectedApps()
        }
    }

    private fun loadApps() {
        if (adbClient == null) {
            Toast.makeText(requireContext(), "请先连接设备", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE

        lifecycleScope.launch {
            val packages = withContext(Dispatchers.IO) {
                adbClient?.listPackages(showSystemApps) ?: emptyList()
            }

            appList.clear()
            packages.forEach { pkg ->
                appList.add(
                    AppInfo(
                        packageName = pkg,
                        appName = pkg.substringAfterLast('.'),
                        isSystem = !pkg.startsWith("com.") || isSystemPackage(pkg)
                    )
                )
            }

            appAdapter?.notifyDataSetChanged()
            binding.progressBar.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
            binding.tvAppCount.text = "共 ${appList.size} 个应用"
        }
    }

    private fun isSystemPackage(pkg: String): Boolean {
        val systemPrefixes = listOf(
            "com.android.", "android.", "com.google.android.",
            "com.sec.", "com.samsung.", "com.huawei.",
            "com.miui.", "com.xiaomi.", "com.oppo.",
            "com.vivo.", "com.meizu."
        )
        return systemPrefixes.any { pkg.startsWith(it) }
    }

    private fun handleAppAction(appInfo: AppInfo, action: String) {
        when (action) {
            "disable" -> disableApp(appInfo)
            "enable" -> enableApp(appInfo)
            "uninstall" -> uninstallApp(appInfo)
            "clear_data" -> clearAppData(appInfo)
        }
    }

    private fun disableApp(appInfo: AppInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle("禁用应用")
            .setMessage("确定要禁用 ${appInfo.displayName} 吗？")
            .setPositiveButton("确定") { _, _ ->
                executeCommand("禁用中...") {
                    adbClient?.disableApp(appInfo.packageName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun enableApp(appInfo: AppInfo) {
        executeCommand("启用中...") {
            adbClient?.enableApp(appInfo.packageName)
        }
    }

    private fun uninstallApp(appInfo: AppInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle("卸载应用")
            .setMessage("确定要卸载 ${appInfo.displayName} 吗？此操作不可恢复！")
            .setPositiveButton("确定") { _, _ ->
                executeCommand("卸载中...") {
                    adbClient?.uninstallApp(appInfo.packageName)
                }
                loadApps()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun clearAppData(appInfo: AppInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle("清除数据")
            .setMessage("确定要清除 ${appInfo.displayName} 的所有数据吗？")
            .setPositiveButton("确定") { _, _ ->
                executeCommand("清除中...") {
                    adbClient?.clearAppData(appInfo.packageName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun uninstallSelectedApps() {
        val selected = appAdapter?.getSelectedApps() ?: emptyList()
        if (selected.isEmpty()) {
            Toast.makeText(requireContext(), "请先选择应用", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("批量卸载")
            .setMessage("确定要卸载选中的 ${selected.size} 个应用吗？")
            .setPositiveButton("确定") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        selected.forEach { app ->
                            adbClient?.uninstallApp(app.packageName)
                        }
                    }
                    binding.progressBar.visibility = View.GONE
                    loadApps()
                    Toast.makeText(requireContext(), "批量操作完成", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun executeCommand(loadingText: String, command: () -> String?) {
        binding.progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                command()
            }
            binding.progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), result ?: "操作完成", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAdbConnectionChanged(client: AdbClient?) {
        adbClient = client
        if (client != null && client.isConnected) {
            loadApps()
        } else {
            appList.clear()
            appAdapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
