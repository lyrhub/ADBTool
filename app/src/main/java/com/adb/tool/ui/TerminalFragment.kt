package com.adb.tool.ui

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.adb.tool.R
import com.adb.tool.core.AdbClient
import com.adb.tool.databinding.FragmentTerminalBinding
import com.adb.tool.utils.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TerminalFragment : Fragment(), MainActivity.OnAdbConnectionListener {

    private var _binding: FragmentTerminalBinding? = null
    private val binding get() = _binding!!
    private var adbClient: AdbClient? = null
    private lateinit var prefs: PrefsManager
    private val commandHistory = mutableListOf<String>()
    private var historyAdapter: ArrayAdapter<String>? = null

    private val quickCommands = listOf(
        "pm list packages" to "列出所有应用",
        "pm list packages -3" to "列出第三方应用",
        "getprop ro.product.model" to "设备型号",
        "getprop ro.build.version.release" to "系统版本",
        "dumpsys battery" to "电池信息",
        "reboot" to "重启设备",
        "screencap -p /sdcard/screenshot.png" to "截图",
        "ls /sdcard/" to "查看SD卡"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTerminalBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())
        setupViews()
        loadHistory()
    }

    private fun setupViews() {
        binding.tvOutput.movementMethod = ScrollingMovementMethod()

        binding.btnExecute.setOnClickListener {
            val command = binding.etCommand.text.toString().trim()
            if (command.isNotEmpty()) {
                executeCommand(command)
            }
        }

        binding.btnClear.setOnClickListener {
            binding.tvOutput.text = ""
        }

        // 快捷命令按钮 - 每行两个
        val quickCommandsLayout = binding.quickCommandsGrid
        var rowLayout: android.widget.LinearLayout? = null
        quickCommands.forEachIndexed { index, (cmd, name) ->
            if (index % 2 == 0) {
                rowLayout = android.widget.LinearLayout(requireContext()).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                quickCommandsLayout.addView(rowLayout)
            }
            val button = android.widget.Button(requireContext()).apply {
                text = name
                textSize = 12f
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                setOnClickListener {
                    binding.etCommand.setText(cmd)
                }
            }
            rowLayout?.addView(button)
        }

        // 历史记录
        historyAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            commandHistory
        )
        binding.lvHistory.adapter = historyAdapter
        binding.lvHistory.onItemClickListener = android.widget.AdapterView.OnItemClickListener { _, _, position, _ ->
            binding.etCommand.setText(commandHistory[position])
        }
    }

    private fun loadHistory() {
        commandHistory.clear()
        commandHistory.addAll(prefs.getCommandHistory())
        historyAdapter?.notifyDataSetChanged()
    }

    private fun executeCommand(command: String) {
        if (adbClient == null) {
            Toast.makeText(requireContext(), "请先连接设备", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnExecute.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    adbClient?.executeCommand(command) ?: "未连接"
                } catch (e: Exception) {
                    "错误: ${e.message}"
                }
            }

            val output = buildString {
                appendLine("> $command")
                appendLine(result)
                appendLine()
                append(binding.tvOutput.text)
            }
            binding.tvOutput.text = output

            prefs.addCommandHistory(command)
            loadHistory()

            binding.btnExecute.isEnabled = true
            binding.progressBar.visibility = View.GONE
            binding.etCommand.text.clear()
        }
    }

    override fun onAdbConnectionChanged(client: AdbClient?) {
        adbClient = client
        if (client != null && client.isConnected) {
            binding.tvOutput.text = "已连接到设备\n"
        } else {
            binding.tvOutput.text = "未连接设备\n"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
