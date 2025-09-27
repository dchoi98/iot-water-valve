/*
    Copyright (C) 2025  Derrick Choi

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

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

    // Adafruit IO credentials
    private val aioUsername = BuildConfig.secretAioUser
    private val aioKey = BuildConfig.secretAioKey
    private val brokerUrl = "ssl://io.adafruit.com:8883"
    private val clientId = "WaterSensorMonitor_${System.currentTimeMillis()}"

    data class SensorEvent(
        val waterDetected: String,
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
                    connectionTimeout = 30
                    keepAliveInterval = 300
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
            val event = SensorEvent(
                waterDetected = data
            )

            serviceScope.launch {
                _globalSensorEvents.emit(event)
            }

            // Update persistent alert state if water detected
            if (event.waterDetected == "true") {
                _alertState.value = event
                showWaterDetectedNotification(event.timestamp)
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showWaterDetectedNotification(timestamp: Long) {
        val formattedTime = java.time.Instant.ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚠️ Water Detected!")
            .setContentText("Valve automatically closed at $formattedTime")
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