package com.kusa.kmllogger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var kmlManager: KmlManager
    private var isLogging = false
    private var isPaused = false

    // バックグラウンドスレッドで位置情報コールバックを受け取るためのHandlerThread
    private var locationHandlerThread: HandlerThread? = null
    private var locationLooper: Looper? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            Log.d("LocationService", "onLocationResult: ${locationResult.locations.size} locations found")
            if (isLogging && !isPaused) {
                for (location in locationResult.locations) {
                    Log.d("LocationService", "Recording interval location: ${location.latitude}, ${location.longitude}")
                    recordLocation(location)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        kmlManager = KmlManager(this)
        createNotificationChannel()

        // 専用バックグラウンドスレッドを起動
        locationHandlerThread = HandlerThread("LocationHandlerThread").also {
            it.start()
            locationLooper = it.looper
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val fileName = intent.getStringExtra(EXTRA_FILE_NAME)
                startLogging(fileName)
            }
            ACTION_PAUSE -> pauseLogging()
            ACTION_RESUME -> resumeLogging()
            ACTION_STOP -> stopLogging()
        }
        return START_NOT_STICKY
    }

    private fun startLogging(fileName: String?) {
        if (isLogging) return
        isLogging = true
        isPaused = false
        Companion.isRunning = true
        Companion.isPaused = false
        kmlManager.startNewLog(fileName)

        val notification = createNotification(getString(R.string.notification_started))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        requestLocationUpdates()
        recordCurrentLocation("START")
        sendStateBroadcast()
    }

    private fun pauseLogging() {
        if (!isLogging || isPaused) return
        recordCurrentLocation("PAUSE")
        isPaused = true
        Companion.isPaused = true
        updateNotification(getString(R.string.notification_paused))
        sendStateBroadcast()
    }

    private fun resumeLogging() {
        if (!isLogging || !isPaused) return
        isPaused = false
        Companion.isPaused = false
        recordCurrentLocation("RESUME")
        updateNotification(getString(R.string.notification_resumed))
        sendStateBroadcast()
    }

    private fun stopLogging() {
        if (!isLogging && !Companion.isRunning) return
        recordCurrentLocation("STOP")
        isLogging = false
        isPaused = false
        Companion.isRunning = false
        Companion.isPaused = false

        fusedLocationClient.removeLocationUpdates(locationCallback)
        kmlManager.finishLog()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        sendStateBroadcast()
        stopSelf()
    }

    private fun recordCurrentLocation(eventLabel: String) {
        Log.d("LocationService", "recordCurrentLocation: event=$eventLabel")
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d("LocationService", "Immediate location found for $eventLabel")
                    recordLocation(location, eventLabel)
                } else {
                    Log.d("LocationService", "No lastLocation available for $eventLabel")
                }
            }
        } catch (e: SecurityException) {
            Log.e("LocationService", "SecurityException in recordCurrentLocation", e)
        }
    }

    private fun recordLocation(location: Location, eventLabel: String? = null) {
        Log.d("LocationService", "recordLocation: ${location.latitude}, ${location.longitude} (event=$eventLabel)")
        kmlManager.appendLocation(location.latitude, location.longitude, location.altitude)

        // 記録中のみ通知を更新（停止処理中の非同期コールバックによる通知復活を防ぐ）
        if (isLogging) {
            updateNotification(getString(R.string.notification_recording, location.latitude, location.longitude))
        }

        // Broadcast for UI
        val intent = Intent(ACTION_LOCATION_UPDATE).apply {
            putExtra(EXTRA_LAT, location.latitude)
            putExtra(EXTRA_LNG, location.longitude)
            putExtra(EXTRA_TIME, SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
            putExtra(EXTRA_EVENT, eventLabel)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    @Suppress("DEPRECATION")
    private fun requestLocationUpdates() {
        Log.d("LocationService", "requestLocationUpdates: Interval=60s")

        val locationRequest = LocationRequest.create().apply {
            interval = 60_000L          // 目標インターバル: 1分
            fastestInterval = 60_000L   // 最小インターバル: 1分（早期配信を防ぐ）
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        val looper = locationLooper ?: Looper.getMainLooper()

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, looper)
        } catch (e: SecurityException) {
            Log.e("LocationService", "SecurityException in requestLocationUpdates", e)
            stopLogging()
        }
    }

    private fun sendStateBroadcast() {
        val intent = Intent(ACTION_STATE_CHANGED).apply {
            putExtra(EXTRA_IS_RUNNING, Companion.isRunning)
            putExtra(EXTRA_IS_PAUSED, Companion.isPaused)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Companion.isRunning = false
        Companion.isPaused = false
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        // HandlerThreadを安全にシャットダウン
        locationHandlerThread?.quitSafely()
        locationHandlerThread = null
        locationLooper = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LocationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.btn_stop),
                stopPendingIntent
            )
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(content: String) {
        if (!isLogging) return
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        var isRunning: Boolean = false
            private set
        var isPaused: Boolean = false
            private set

        const val ACTION_START = "com.kusa.kmllogger.START"
        const val ACTION_PAUSE = "com.kusa.kmllogger.PAUSE"
        const val ACTION_RESUME = "com.kusa.kmllogger.RESUME"
        const val ACTION_STOP = "com.kusa.kmllogger.STOP"
        const val EXTRA_FILE_NAME = "extra_file_name"

        const val ACTION_LOCATION_UPDATE = "com.kusa.kmllogger.LOCATION_UPDATE"
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_TIME = "extra_time"
        const val EXTRA_EVENT = "extra_event"

        const val ACTION_STATE_CHANGED = "com.kusa.kmllogger.STATE_CHANGED"
        const val EXTRA_IS_RUNNING = "extra_is_running"
        const val EXTRA_IS_PAUSED = "extra_is_paused"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "gps_logger_channel"
    }
}
