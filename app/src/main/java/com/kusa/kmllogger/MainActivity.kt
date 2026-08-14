package com.kusa.kmllogger

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var etFileName: EditText
    private lateinit var btnStart: Button
    private lateinit var btnPauseResume: Button
    private lateinit var btnStop: Button
    private lateinit var tvStatus: TextView
    private lateinit var rvLog: RecyclerView
    private lateinit var logAdapter: LogAdapter

    private var isPaused = false

    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra(LocationService.EXTRA_LAT, 0.0) ?: 0.0
            val lng = intent?.getDoubleExtra(LocationService.EXTRA_LNG, 0.0) ?: 0.0
            val time = intent?.getStringExtra(LocationService.EXTRA_TIME) ?: ""
            val event = intent?.getStringExtra(LocationService.EXTRA_EVENT)
            
            logAdapter.addEntry(LogEntry(time, lat, lng, event))
            rvLog.scrollToPosition(0)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (granted) {
            checkBackgroundLocationPermission()
        } else {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val backgroundPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startLoggingService()
        } else {
            Toast.makeText(this, "Background location is recommended for screen-off logging", Toast.LENGTH_LONG).show()
            startLoggingService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        etFileName = findViewById(R.id.etFileName)
        btnStart = findViewById(R.id.btnStart)
        btnPauseResume = findViewById(R.id.btnPauseResume)
        btnStop = findViewById(R.id.btnStop)
        tvStatus = findViewById(R.id.tvStatus)
        rvLog = findViewById(R.id.rvLog)

        logAdapter = LogAdapter()
        rvLog.layoutManager = LinearLayoutManager(this)
        rvLog.adapter = logAdapter

        btnStart.setOnClickListener { 
            checkPermissionsAndStart()
        }

        btnPauseResume.setOnClickListener { 
            if (isPaused) {
                resumeLogging()
            } else {
                pauseLogging()
            }
        }

        btnStop.setOnClickListener { 
            stopLogging()
        }
    }

    override fun onStart() {
        super.onStart()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            locationReceiver,
            IntentFilter(LocationService.ACTION_LOCATION_UPDATE)
        )
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver)
    }

    private fun checkPermissionsAndStart() {
        val requiredPermissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 既に全て付与済みなら直接起動（ダイアログを出さない）
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            checkBackgroundLocationPermission()
        } else {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    private fun checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showBackgroundPermissionDialog()
            } else {
                startLoggingService()
            }
        } else {
            startLoggingService()
        }
    }

    private fun showBackgroundPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Background Location Required")
            .setMessage("To record GPS even when the screen is off, please select 'Allow all the time' in the next screen.")
            .setPositiveButton("Settings") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
            .setNegativeButton("Maybe Later") { _, _ ->
                startLoggingService()
            }
            .show()
    }

    private fun startLoggingService() {
        logAdapter.clear()
        val fileName = etFileName.text.toString()
        val intent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_START
            putExtra(LocationService.EXTRA_FILE_NAME, fileName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        updateUi(true)
    }

    private fun pauseLogging() {
        val intent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_PAUSE
        }
        startService(intent)
        isPaused = true
        btnPauseResume.text = "Resume"
        tvStatus.text = "Status: Paused"
    }

    private fun resumeLogging() {
        val intent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_RESUME
        }
        startService(intent)
        isPaused = false
        btnPauseResume.text = "Pause"
        tvStatus.text = "Status: Logging"
    }

    private fun stopLogging() {
        val intent = Intent(this, LocationService::class.java).apply {
            action = LocationService.ACTION_STOP
        }
        startService(intent)
        updateUi(false)
    }

    private fun updateUi(isLogging: Boolean) {
        if (isLogging) {
            btnStart.visibility = View.GONE
            btnPauseResume.visibility = View.VISIBLE
            btnStop.visibility = View.VISIBLE
            etFileName.isEnabled = false
            tvStatus.text = "Status: Logging"
        } else {
            btnStart.visibility = View.VISIBLE
            btnPauseResume.visibility = View.GONE
            btnStop.visibility = View.GONE
            etFileName.isEnabled = true
            tvStatus.text = "Status: Idle"
            isPaused = false
            btnPauseResume.text = "Pause"
        }
    }
}
