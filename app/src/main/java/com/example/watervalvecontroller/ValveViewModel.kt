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

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ValveViewModel(application: Application) : AndroidViewModel(application) {

    private val valveService = ValveService()

    sealed class UiState {
        data object Ready : UiState()
        data object OpeningValve : UiState()
        data object ClosingValve : UiState()
        data class SensorAlert(val timestamp: Long) : UiState()
        data class Success(val message: String) : UiState()
        data class Error(val message: String) : UiState()
    }

    sealed class ValveAction {
        data object OpenValve : ValveAction()
        data object CloseValve : ValveAction()
        data object ClearStatus : ValveAction()
        data object DismissAlert : ValveAction()
        data object StartMonitoring : ValveAction()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Ready)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)

    init {
        startSensorMonitoring()

        checkExistingAlertState()

        observeSensorEvents()

        observeAlertState()
    }

    private fun checkExistingAlertState() {
        viewModelScope.launch {
            val alertEvent = SensorMonitorService.alertState.value
            if (alertEvent != null) {
                _uiState.value = UiState.SensorAlert(alertEvent.timestamp)
            }
        }
    }

    private fun observeAlertState() {
        SensorMonitorService.alertState
            .onEach { alertEvent ->
                if (alertEvent != null) {
                    _uiState.value = UiState.SensorAlert(alertEvent.timestamp)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observeSensorEvents() {
        SensorMonitorService.globalSensorEvents
            .onEach { event ->
                // Update UI state when water detected (reading >= 1000 or triggered flag)
                if (event.reading >= 1000) {
                    _uiState.value = UiState.SensorAlert(event.timestamp)
                }
            }
            .launchIn(viewModelScope)
    }

    fun handleAction(action: ValveAction) {
        when (action) {
            is ValveAction.OpenValve -> openValve()
            is ValveAction.CloseValve -> closeValve()
            is ValveAction.ClearStatus -> clearStatus()
            is ValveAction.DismissAlert -> dismissAlert()
            is ValveAction.StartMonitoring -> startSensorMonitoring()
        }
    }

    private fun startSensorMonitoring() {
        val context = getApplication<Application>()
        val intent = Intent(context, SensorMonitorService::class.java)
        context.startForegroundService(intent)
        _isMonitoring.value = true
    }

    private fun openValve() {
        _uiState.value = UiState.OpeningValve

        viewModelScope.launch {
            val result = valveService.openValve()

            _uiState.value = when (result) {
                is ValveService.ValveResult.Success -> UiState.Success("Opened valve")
                is ValveService.ValveResult.Error -> UiState.Error(result.message)
            }

            kotlinx.coroutines.delay(3000)
            if (_uiState.value is UiState.Success || _uiState.value is UiState.Error) {
                _uiState.value = UiState.Ready
            }
        }
    }

    private fun closeValve() {
        _uiState.value = UiState.ClosingValve

        viewModelScope.launch {
            val result = valveService.closeValve()

            _uiState.value = when (result) {
                is ValveService.ValveResult.Success -> UiState.Success("Closed valve")
                is ValveService.ValveResult.Error -> UiState.Error(result.message)
            }

            kotlinx.coroutines.delay(3000)
            if (_uiState.value is UiState.Success || _uiState.value is UiState.Error) {
                _uiState.value = UiState.Ready
            }
        }
    }

    private fun clearStatus() {
        _uiState.value = UiState.Ready
    }

    private fun dismissAlert() {
        SensorMonitorService.clearAlert()
        _uiState.value = UiState.Ready
    }

    override fun onCleared() {
        super.onCleared()
        valveService.cleanup()
    }
}