package com.example.watervalvecontroller

import io.ktor.client.*

import io.ktor.client.engine.android.*

import io.ktor.client.request.*

import io.ktor.client.statement.*

class ValveService(private val baseUrl: String = "http://192.168.0.206") {
    // Create HTTP client with Android engine
    private val client = HttpClient(Android) {
        engine {
            // Configure connection timeout
            connectTimeout = 5_000
            socketTimeout = 10_000
        }
    }

    sealed class ValveResult {
        data object Success : ValveResult()
        data class Error(val message: String) : ValveResult()
    }

    suspend fun openValve(): ValveResult {
        return executeValveOperation("/open")
    }

    suspend fun closeValve(): ValveResult {
        return executeValveOperation("/close")
    }

    private suspend fun executeValveOperation(endpoint: String): ValveResult {
        return try {
            val response: HttpResponse = client.post("$baseUrl$endpoint")

            // Check if response indicates success (200-299 status codes)
            if (response.status.value in 200..299) {
                ValveResult.Success
            } else {
                ValveResult.Error("Server returned ${response.status.value}")
            }
        } catch (e: Exception) {
            // Convert any exception into our domain-specific error type
            ValveResult.Error("Connection failed: ${e.localizedMessage}")
        }
    }

    fun cleanup() {
        client.close()
    }
}