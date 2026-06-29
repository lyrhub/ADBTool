package com.adb.tool.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.adb.tool.R
import com.adb.tool.core.AdbClient
import com.adb.tool.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    var adbClient: AdbClient? = null
        private set

    private val fragments: List<Fragment> = listOf(
        ConnectFragment(),
        AppsFragment(),
        TerminalFragment(),
        FilesFragment()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupBottomNavigation()
    }

    private fun setupViewPager() {
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = fragments.size
            override fun createFragment(position: Int) = fragments[position]
        }
        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.bottomNavigation.menu.getItem(position).isChecked = true
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_connect -> {
                    binding.viewPager.currentItem = 0
                    true
                }
                R.id.nav_apps -> {
                    binding.viewPager.currentItem = 1
                    true
                }
                R.id.nav_terminal -> {
                    binding.viewPager.currentItem = 2
                    true
                }
                R.id.nav_files -> {
                    binding.viewPager.currentItem = 3
                    true
                }
                else -> false
            }
        }
    }

    fun setAdbClient(client: AdbClient?) {
        adbClient = client
        fragments.forEach { fragment ->
            if (fragment is OnAdbConnectionListener) {
                fragment.onAdbConnectionChanged(client)
            }
        }
    }

    fun switchToPage(index: Int) {
        if (index in fragments.indices) {
            binding.viewPager.currentItem = index
        }
    }

    interface OnAdbConnectionListener {
        fun onAdbConnectionChanged(client: AdbClient?)
    }

    override fun onDestroy() {
        super.onDestroy()
        adbClient?.disconnect()
    }
}
