package com.example.watervalvecontroller

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class SensorMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "water_sensor_channel"
        const val NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_ID = 2
        const val ACTION_STOP = "com.example.watervalvecontroller.STOP_SERVICE"

        // Persistent alert state - survives ViewModel recreation
        private val _alertState = MutableStateFlow<SensorEvent?>(null)
        val alertState: StateFlow<SensorEvent?> = _alertState.asStateFlow()

        // Singleton flow for sharing sensor events across the app
        private val _globalSensorEvents = MutableSharedFlow<SensorEvent>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val globalSensorEvents: SharedFlow<SensorEvent> = _globalSensorEvents.asSharedFlow()

        // Function to clear alert state (called from ViewModel)
        fun clearAlert() {
            _alertState.value = null
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var eventSource: EventSource? = null
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private var lastNotificationReading = 0

    data class SensorEvent(
        val reading: Int,
        val triggered: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        connectToSSE()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Water Sensor Monitoring",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Monitors water sensor and alerts when water detected"
            setShowBadge(true)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Water Sensor Active")
            .setContentText("Monitoring for water detection")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun connectToSSE() {
        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MINUTES) // No timeout for SSE
            .build()

        val request = Request.Builder()
            .url("http://192.168.0.206/events")
            .header("Accept", "text/event-stream")
            .build()

        val listener = createEventSourceListener()
        eventSource = EventSources.createFactory(client).newEventSource(request, listener)
    }

    private fun createEventSourceListener() = object : EventSourceListener() {
        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            when (type) {
                "sensor" -> handleSensorEvent(data)
            }
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            // Retry connection after delay
            serviceScope.launch {
                delay(5000)
                connectToSSE()
            }
        }
    }

    private fun handleSensorEvent(data: String) {
        try {
            val json = JSONObject(data)
            val event = SensorEvent(
                reading = json.getInt("reading"),
                triggered = json.getBoolean("triggered")
            )

            // Emit event to global Flow for ViewModel consumption
            serviceScope.launch {
                _globalSensorEvents.emit(event)
            }

            // Update persistent alert state if water detected
            if (event.reading >= 1000 || event.triggered) {
                _alertState.value = event
            }

            // Show alert notification if water detected
            if (event.reading >= 1000 && lastNotificationReading < 1000) {
                showWaterDetectedNotification()
            }

            lastNotificationReading = event.reading

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showWaterDetectedNotification() {
        val currentDateTime = LocalDateTime.now().format(dateTimeFormatter)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ Water Detected!")
            .setContentText("Valve automatically closed $currentDateTime")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 100, 500))
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        eventSource?.cancel()
        serviceScope.cancel()
    }
}