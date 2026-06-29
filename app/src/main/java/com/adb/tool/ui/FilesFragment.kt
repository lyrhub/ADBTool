package com.adb.tool.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.adb.tool.core.AdbClient
import com.adb.tool.core.FileItem
import com.adb.tool.databinding.FragmentFilesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FilesFragment : Fragment(), MainActivity.OnAdbConnectionListener {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private var adbClient: AdbClient? = null
    private val fileList = mutableListOf<FileItem>()
    private var fileAdapter: FileListAdapter? = null
    private var currentPath = "/sdcard"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        fileAdapter = FileListAdapter(fileList) { fileItem ->
            if (fileItem.isDirectory) {
                navigateTo(fileItem.path)
            } else {
                Toast.makeText(requireContext(), "文件: ${fileItem.name}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = fileAdapter
        }

        binding.btnBack.setOnClickListener {
            navigateUp()
        }

        binding.btnRefresh.setOnClickListener {
            loadFiles()
        }

        binding.btnHome.setOnClickListener {
            currentPath = "/sdcard"
            loadFiles()
        }
    }

    private fun navigateTo(path: String) {
        currentPath = path
        loadFiles()
    }

    private fun navigateUp() {
        if (currentPath == "/" || currentPath.isEmpty()) return
        val parent = currentPath.substringBeforeLast('/')
        currentPath = if (parent.isEmpty()) "/" else parent
        loadFiles()
    }

    private fun loadFiles() {
        if (adbClient == null) {
            Toast.makeText(requireContext(), "请先连接设备", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.tvPath.text = currentPath

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                adbClient?.listFiles(currentPath) ?: ""
            }

            fileList.clear()
            val lines = result.lines()
            lines.forEach { line ->
                if (line.isNotEmpty() && !line.startsWith("total")) {
                    val parts = line.split("\\s+".toRegex())
                    if (parts.size >= 9) {
                        val isDir = parts[0].startsWith('d')
                        val name = parts.drop(8).joinToString(" ")
                        val size = parts[4].toLongOrNull() ?: 0
                        val date = "${parts[5]} ${parts[6]} ${parts[7]}"
                        val permission = parts[0]
                        
                        if (name != "." && name != "..") {
                            fileList.add(
                                FileItem(
                                    name = name,
                                    path = "$currentPath/$name",
                                    isDirectory = isDir,
                                    size = size,
                                    date = date,
                                    permission = permission
                                )
                            )
                        }
                    }
                }
            }

            fileList.sortWith(compareByDescending<FileItem> { it.isDirectory }.thenBy { it.name })

            fileAdapter?.notifyDataSetChanged()
            binding.progressBar.visibility = View.GONE
            binding.tvFileCount.text = "${fileList.size} 项"
        }
    }

    override fun onAdbConnectionChanged(client: AdbClient?) {
        adbClient = client
        if (client != null && client.isConnected) {
            loadFiles()
        } else {
            fileList.clear()
            fileAdapter?.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
