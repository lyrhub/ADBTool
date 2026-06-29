package com.adb.tool.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.adb.tool.R
import com.adb.tool.core.AdbClient
import com.adb.tool.databinding.FragmentConnectBinding
import com.adb.tool.utils.NetworkUtils
import com.adb.tool.utils.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConnectFragment : Fragment(), MainActivity.OnAdbConnectionListener {

    private var _binding: FragmentConnectBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: PrefsManager
    private var adbClient: AdbClient? = null
    private val scannedDevices = mutableListOf<String>()
    private var deviceAdapter: ArrayAdapter<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConnectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = PrefsManager(requireContext())
        setupViews()
        loadSavedSettings()
    }

    private fun setupViews() {
        binding.btnConnect.setOnClickListener {
            val ip = binding.etIp.text.toString().trim()
            val port = binding.etPort.text.toString().toIntOrNull() ?: 5555
            if (ip.isEmpty()) {
                Toast.makeText(requireContext(), "请输入IP地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            connectToDevice(ip, port)
        }

        binding.btnDisconnect.setOnClickListener {
            disconnect()
        }

        binding.btnScan.setOnClickListener {
            scanNetwork()
        }

        deviceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            scannedDevices
        )
        binding.lvDevices.adapter = deviceAdapter
        binding.lvDevices.onItemClickListener = android.widget.AdapterView.OnItemClickListener { _, _, position, _ ->
            val ip = scannedDevices[position]
            binding.etIp.setText(ip)
        }
    }

    private fun loadSavedSettings() {
        if (prefs.lastIp.isNotEmpty()) {
            binding.etIp.setText(prefs.lastIp)
            binding.etPort.setText(prefs.lastPort.toString())
        }
    }

    private fun scanNetwork() {
        binding.btnScan.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        scannedDevices.clear()
        deviceAdapter?.notifyDataSetChanged()

        lifecycleScope.launch {
            val subnet = NetworkUtils.getSubnet()
            binding.tvScanStatus.text = "正在扫描网段: $subnet.*"
            
            withContext(Dispatchers.IO) {
                NetworkUtils.scanNetwork(subnet) { ip ->
                    activity?.runOnUiThread {
                        if (!scannedDevices.contains(ip)) {
                            scannedDevices.add(ip)
                            scannedDevices.sort()
                            deviceAdapter?.notifyDataSetChanged()
                        }
                    }
                }
            }

            binding.btnScan.isEnabled = true
            binding.progressBar.visibility = View.GONE
            binding.tvScanStatus.text = "扫描完成，发现 ${scannedDevices.size} 台设备"
        }
    }

    private fun connectToDevice(ip: String, port: Int) {
        binding.btnConnect.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "正在连接..."

        lifecycleScope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val client = AdbClient(ip, port)
                    val connected = client.connect()
                    if (connected) {
                        adbClient = client
                        true
                    } else {
                        false
                    }
                } catch (e: Exception) {
                    false
                }
            }

            binding.btnConnect.isEnabled = true
            binding.progressBar.visibility = View.GONE

            if (success) {
                binding.tvStatus.text = "已连接: $ip:$port"
                binding.btnDisconnect.visibility = View.VISIBLE
                binding.btnConnect.visibility = View.GONE
                
                prefs.lastIp = ip
                prefs.lastPort = port

                (activity as? MainActivity)?.setAdbClient(adbClient)
                Toast.makeText(requireContext(), "连接成功", Toast.LENGTH_SHORT).show()
            } else {
                binding.tvStatus.text = "连接失败"
                Toast.makeText(requireContext(), "连接失败，请检查IP和端口", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun disconnect() {
        adbClient?.disconnect()
        adbClient = null
        (activity as? MainActivity)?.setAdbClient(null)
        
        binding.tvStatus.text = "未连接"
        binding.btnDisconnect.visibility = View.GONE
        binding.btnConnect.visibility = View.VISIBLE
        Toast.makeText(requireContext(), "已断开连接", Toast.LENGTH_SHORT).show()
    }

    override fun onAdbConnectionChanged(client: AdbClient?) {
        adbClient = client
        if (client != null && client.isConnected) {
            binding.tvStatus.text = "已连接"
            binding.btnDisconnect.visibility = View.VISIBLE
            binding.btnConnect.visibility = View.GONE
        } else {
            binding.tvStatus.text = "未连接"
            binding.btnDisconnect.visibility = View.GONE
            binding.btnConnect.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
