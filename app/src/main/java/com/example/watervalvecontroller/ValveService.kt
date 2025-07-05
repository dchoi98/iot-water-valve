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

import kotlinx.coroutines.suspendCancellableCoroutine
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import kotlin.coroutines.resume

class ValveService(
    private val aioUsername: String = "myUsername",
    private val aioKey: String = "myKey"
) {
    private val brokerUrl = "ssl://io.adafruit.com:8883"
    private val clientId = "WaterValveController_${System.currentTimeMillis()}"
    private var mqttClient: MqttClient? = null

    sealed class ValveResult {
        data object Success : ValveResult()
        data class Error(val message: String) : ValveResult()
    }

    private suspend fun ensureConnection(): ValveResult = suspendCancellableCoroutine { continuation ->
        try {
            if (mqttClient?.isConnected == true) {
                continuation.resume(ValveResult.Success)
                return@suspendCancellableCoroutine
            }

            mqttClient = MqttClient(brokerUrl, clientId, MemoryPersistence())

            val options = MqttConnectOptions().apply {
                userName = aioUsername
                password = aioKey.toCharArray()
                isCleanSession = true
                connectionTimeout = 10
                keepAliveInterval = 30
            }

            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {}
                override fun messageArrived(topic: String?, message: MqttMessage?) {}
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })

            // Use synchronous connect
            mqttClient?.connect(options)
            continuation.resume(ValveResult.Success)

        } catch (e: Exception) {
            continuation.resume(ValveResult.Error("Connection error: ${e.message}"))
        }
    }

    private suspend fun publishCommand(command: String): ValveResult = suspendCancellableCoroutine { continuation ->
        try {
            val topic = "$aioUsername/feeds/valve-control"
            val payload = command.toByteArray()
            val qos = 1
            val retained = false

            mqttClient?.publish(topic, payload, qos, retained)
            continuation.resume(ValveResult.Success)

        } catch (e: Exception) {
            continuation.resume(ValveResult.Error("Command error: ${e.message}"))
        }
    }

    suspend fun openValve(): ValveResult {
        return when (val connectionResult = ensureConnection()) {
            is ValveResult.Success -> publishCommand("open")
            is ValveResult.Error -> connectionResult
        }
    }

    suspend fun closeValve(): ValveResult {
        return when (val connectionResult = ensureConnection()) {
            is ValveResult.Success -> publishCommand("close")
            is ValveResult.Error -> connectionResult
        }
    }

    fun cleanup() {
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}