package com.example.watervalvecontroller

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Foreground service that monitors water sensor via MQTT
 */
class SensorMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "water_sensor_channel"
        const val NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_ID = 2
        const val ACTION_STOP = "com.example.watervalvecontroller.STOP_SERVICE"

        private val _alertState = MutableStateFlow<SensorEvent?>(null)
        val alertState: StateFlow<SensorEvent?> = _alertState.asStateFlow()

        private val _globalSensorEvents = MutableSharedFlow<SensorEvent>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
        val globalSensorEvents: SharedFlow<SensorEvent> = _globalSensorEvents.asSharedFlow()

        fun clearAlert() {
            _alertState.value = null
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var mqttClient: MqttClient? = null
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private var lastNotificationReading = 0

    // Adafruit IO credentials
    private val aioUsername = "myUsername"
    private val aioKey = "myKey"
    private val brokerUrl = "ssl://io.adafruit.com:8883"
    private val clientId = "WaterSensorMonitor_${System.currentTimeMillis()}"

    data class SensorEvent(
        val reading: Int,
        val triggered: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createForegroundNotification())
        connectToMQTT()
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
            description = "Monitors water sensor via Adafruit IO"
            setShowBadge(true)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Water Sensor Active")
            .setContentText("Monitoring via Adafruit IO")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun connectToMQTT() {
        serviceScope.launch {
            try {
                mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())

                val options = MqttConnectOptions().apply {
                    userName = aioUsername
                    password = aioKey.toCharArray()
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 30
                }

                mqttClient?.setCallback(createMqttCallback())
                mqttClient?.connect(options)

                // Subscribe to sensor feed
                val topic = "$aioUsername/feeds/water-sensor"
                mqttClient?.subscribe(topic, 1)

            } catch (e: Exception) {
                e.printStackTrace()
                // Retry connection after delay
                delay(5000)
                connectToMQTT()
            }
        }
    }

    private fun createMqttCallback() = object : MqttCallback {
        override fun connectionLost(cause: Throwable?) {
            serviceScope.launch {
                delay(5000)
                connectToMQTT()
            }
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            message?.let { handleSensorMessage(String(it.payload)) }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
    }

    private fun handleSensorMessage(data: String) {
        try {
            val json = JSONObject(data)
            val event = SensorEvent(
                reading = json.getInt("reading"),
                triggered = json.getBoolean("triggered")
            )

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
            .setContentText("Valve automatically closed at $currentDateTime")
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
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
        serviceScope.cancel()
    }
}