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
import kotlinx.coroutines.withTimeout
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
    private val ackTopic = "$aioUsername/feeds/valve-ack"

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

            mqttClient?.connect(options)
            mqttClient?.subscribe(ackTopic, 1)
            continuation.resume(ValveResult.Success)

        } catch (e: Exception) {
            continuation.resume(ValveResult.Error("Connection error: ${e.message}"))
        }
    }

    private suspend fun publishCommandWithAck(command: String): ValveResult {
        return try {
            withTimeout(5000L) {
                suspendCancellableCoroutine { continuation ->
                    val expectedAck = if (command == "open") "opened" else "closed"

                    mqttClient?.setCallback(object : MqttCallback {
                        override fun connectionLost(cause: Throwable?) {}

                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            if (topic == ackTopic && message?.toString() == expectedAck) {
                                continuation.resume(ValveResult.Success)
                            }
                        }

                        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                    })

                    // Publish command
                    val topic = "$aioUsername/feeds/valve-control"
                    mqttClient?.publish(topic, command.toByteArray(), 1, false)
                }
            }
        } catch (e: Exception) {
            ValveResult.Error("Timeout waiting for valve acknowledgment")
        }
    }

    suspend fun openValve(): ValveResult {
        return when (val connectionResult = ensureConnection()) {
            is ValveResult.Success -> publishCommandWithAck("open")
            is ValveResult.Error -> connectionResult
        }
    }

    suspend fun closeValve(): ValveResult {
        return when (val connectionResult = ensureConnection()) {
            is ValveResult.Success -> publishCommandWithAck("close")
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