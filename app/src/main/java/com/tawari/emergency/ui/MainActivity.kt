package com.tawari.emergency.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tawari.emergency.R
import com.tawari.emergency.databinding.ActivityMainBinding
import com.tawari.emergency.models.*
import com.tawari.emergency.network.ApiClient
import com.tawari.emergency.services.NotificationService
import com.tawari.emergency.ui.adapters.*
import com.tawari.emergency.utils.LocationHelper
import com.tawari.emergency.utils.PrefsManager
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var apiClient: ApiClient
    private lateinit var locationHelper: LocationHelper
    private lateinit var prefsManager: PrefsManager

    private var currentTab = 0
    private var isConnected = false
    private var currentLocation: String = ""
    private var connectionJob: Job? = null

    private val reportsAdapter = ReportsAdapter()
    private val agentsAdapter = AgentsAdapter { agent -> openChat(agent) }
    private val contentAdapter = ContentAdapter()

    // Permission launcher
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            getLocation()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Notifications permission handled */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiClient = ApiClient(this)
        locationHelper = LocationHelper(this)
        prefsManager = PrefsManager(this)

        setupUI()
        requestPermissions()
        startConnectionMonitor()
    }

    private fun setupUI() {
        // Setup RecyclerViews
        binding.reportsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = reportsAdapter
        }

        binding.agentsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = agentsAdapter
        }

        binding.contentRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = contentAdapter
        }

        // Tab buttons
        binding.tabEmergency.setOnClickListener { switchTab(0) }
        binding.tabReports.setOnClickListener { switchTab(1) }
        binding.tabContact.setOnClickListener { switchTab(2) }
        binding.tabInfo.setOnClickListener { switchTab(3) }

        // SOS Button
        binding.sosButton.setOnClickListener { sendSOS() }

        // Emergency Type Buttons
        binding.btnFire.setOnClickListener { sendEmergency(EmergencyType.FIRE) }
        binding.btnInjury.setOnClickListener { sendEmergency(EmergencyType.INJURY) }
        binding.btnThreat.setOnClickListener { sendEmergency(EmergencyType.THREAT) }
        binding.btnAccident.setOnClickListener { sendEmergency(EmergencyType.ACCIDENT) }
        binding.btnTrapped.setOnClickListener { sendEmergency(EmergencyType.TRAPPED) }
        binding.btnOther.setOnClickListener { sendEmergency(EmergencyType.OTHER) }

        // Swipe to refresh
        binding.swipeRefresh.setOnRefreshListener {
            refreshData()
        }

        // Initial tab
        switchTab(0)
        updateConnectionStatus(false)
    }

    private fun switchTab(tab: Int) {
        currentTab = tab

        // Reset all tabs
        listOf(binding.tabEmergency, binding.tabReports, binding.tabContact, binding.tabInfo)
            .forEach { it.isSelected = false }

        // Hide all content
        binding.emergencyContent.visibility = View.GONE
        binding.reportsContent.visibility = View.GONE
        binding.contactContent.visibility = View.GONE
        binding.infoContent.visibility = View.GONE

        // Show selected
        when (tab) {
            0 -> {
                binding.tabEmergency.isSelected = true
                binding.emergencyContent.visibility = View.VISIBLE
            }
            1 -> {
                binding.tabReports.isSelected = true
                binding.reportsContent.visibility = View.VISIBLE
                loadReports()
            }
            2 -> {
                binding.tabContact.isSelected = true
                binding.contactContent.visibility = View.VISIBLE
                loadAgents()
            }
            3 -> {
                binding.tabInfo.isSelected = true
                binding.infoContent.visibility = View.VISIBLE
                loadContent()
            }
        }
    }

    private fun requestPermissions() {
        // Location permission
        if (!locationHelper.hasLocationPermission()) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            getLocation()
        }

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun getLocation() {
        lifecycleScope.launch {
            val location = locationHelper.getCurrentLocation()
            currentLocation = locationHelper.formatLocation(location)
            if (currentLocation.isNotEmpty()) {
                prefsManager.savedLocation = currentLocation
                updateLocationUI(true)
            }
        }
    }

    private fun updateLocationUI(hasLocation: Boolean) {
        binding.locationStatus.text = if (hasLocation) {
            "تم تحديد موقعك"
        } else {
            "يرجى السماح بالوصول للموقع"
        }
        binding.locationIcon.setImageResource(
            if (hasLocation) R.drawable.ic_location_on else R.drawable.ic_location_off
        )
    }

    private fun startConnectionMonitor() {
        connectionJob = lifecycleScope.launch {
            while (isActive) {
                checkConnection()
                delay(5000) // Check every 5 seconds
            }
        }
    }

    private suspend fun checkConnection() {
        val isVillageNetwork = apiClient.isConnectedToVillageNetwork()
        
        if (isVillageNetwork) {
            val nodeIp = apiClient.findEmergencyNode()
            isConnected = nodeIp != null
            
            withContext(Dispatchers.Main) {
                updateConnectionStatus(isConnected)
                if (isConnected) {
                    binding.networkName.text = apiClient.getCurrentSSID() ?: "شبكة القرية"
                }
            }
        } else {
            isConnected = false
            withContext(Dispatchers.Main) {
                updateConnectionStatus(false)
                showNotConnectedMessage()
            }
        }
    }

    private fun updateConnectionStatus(connected: Boolean) {
        binding.connectionStatus.text = if (connected) "متصل" else "غير متصل"
        binding.connectionDot.setBackgroundResource(
            if (connected) R.drawable.dot_green else R.drawable.dot_red
        )
        binding.sosButton.isEnabled = connected
        binding.sosButton.alpha = if (connected) 1f else 0.5f
    }

    private fun showNotConnectedMessage() {
        binding.notConnectedCard.visibility = if (!isConnected) View.VISIBLE else View.GONE
    }

    private fun sendSOS() {
        if (!isConnected) {
            Toast.makeText(this, "يجب الاتصال بشبكة القرية أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("تأكيد الطوارئ")
            .setMessage("هل تريد إرسال بلاغ طوارئ فوري؟")
            .setPositiveButton("إرسال") { _, _ ->
                sendEmergency(EmergencyType.SOS)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun sendEmergency(type: EmergencyType) {
        if (!isConnected) {
            Toast.makeText(this, "يجب الاتصال بشبكة القرية أولاً", Toast.LENGTH_SHORT).show()
            return
        }

        // Check location
        if (currentLocation.isEmpty()) {
            currentLocation = prefsManager.savedLocation
        }

        if (currentLocation.isEmpty()) {
            showLocationRequiredDialog(type)
            return
        }

        // Show note dialog for non-SOS emergencies
        if (type != EmergencyType.SOS) {
            showNoteDialog(type)
        } else {
            doSendEmergency(type, "")
        }
    }

    private fun showLocationRequiredDialog(type: EmergencyType) {
        MaterialAlertDialogBuilder(this)
            .setTitle("الموقع غير محدد")
            .setMessage("لم يتم تحديد موقعك. هل تريد الإرسال بدون موقع؟")
            .setPositiveButton("إرسال") { _, _ ->
                if (type != EmergencyType.SOS) {
                    showNoteDialog(type)
                } else {
                    doSendEmergency(type, "")
                }
            }
            .setNegativeButton("إلغاء", null)
            .setNeutralButton("إعادة تحديد الموقع") { _, _ ->
                getLocation()
            }
            .show()
    }

    private fun showNoteDialog(type: EmergencyType) {
        val input = android.widget.EditText(this).apply {
            hint = "ملاحظة إضافية (اختياري)"
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("بلاغ ${type.arabicName}")
            .setView(input)
            .setPositiveButton("إرسال") { _, _ ->
                doSendEmergency(type, input.text.toString())
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun doSendEmergency(type: EmergencyType, note: String) {
        binding.loadingOverlay.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = apiClient.sendEmergencyReport(
                type = type.arabicName,
                location = currentLocation,
                note = note
            )

            withContext(Dispatchers.Main) {
                binding.loadingOverlay.visibility = View.GONE

                result.onSuccess {
                    // Save locally
                    prefsManager.saveReport(
                        EmergencyReport(
                            id = System.currentTimeMillis().toInt(),
                            type = type.arabicName,
                            location = currentLocation,
                            note = note,
                            status = 0,
                            synced = true
                        )
                    )

                    // Show notification
                    NotificationService.showEmergencyNotification(
                        this@MainActivity,
                        "تم إرسال البلاغ",
                        "بلاغ ${type.arabicName} في الانتظار"
                    )

                    showSuccessDialog()
                }

                result.onFailure { e ->
                    Toast.makeText(
                        this@MainActivity,
                        "فشل الإرسال: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showSuccessDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("تم الإرسال")
            .setMessage("تم إرسال بلاغك بنجاح. سيتم التواصل معك قريباً.")
            .setPositiveButton("حسناً", null)
            .show()
    }

    private fun loadReports() {
        val reports = prefsManager.getReports()
        reportsAdapter.submitList(reports)
        binding.emptyReports.visibility = if (reports.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadAgents() {
        if (!isConnected) {
            binding.emptyAgents.text = "غير متصل بالشبكة"
            binding.emptyAgents.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            val result = apiClient.getAgents()
            withContext(Dispatchers.Main) {
                result.onSuccess { agents ->
                    agentsAdapter.submitList(agents)
                    binding.emptyAgents.visibility = if (agents.isEmpty()) View.VISIBLE else View.GONE
                }
                result.onFailure {
                    binding.emptyAgents.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadContent() {
        if (!isConnected) {
            binding.emptyContent.text = "غير متصل بالشبكة"
            binding.emptyContent.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch {
            val result = apiClient.getContent()
            withContext(Dispatchers.Main) {
                result.onSuccess { content ->
                    contentAdapter.submitList(content)
                    binding.emptyContent.visibility = if (content.isEmpty()) View.VISIBLE else View.GONE
                }
                result.onFailure {
                    binding.emptyContent.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun refreshData() {
        lifecycleScope.launch {
            checkConnection()
            when (currentTab) {
                1 -> loadReports()
                2 -> loadAgents()
                3 -> loadContent()
            }
            binding.swipeRefresh.isRefreshing = false
        }
    }

    private fun openChat(agent: Agent) {
        if (!agent.isAvailable) {
            Toast.makeText(this, "العميل غير متاح حالياً", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = android.content.Intent(this, ChatActivity::class.java).apply {
            putExtra("agent_name", agent.name)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionJob?.cancel()
    }
}
