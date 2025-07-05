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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val viewModel: ValveViewModel by viewModels()

    // Notification permission launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.handleAction(ValveViewModel.ValveAction.StartMonitoring)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        setContent {
            ValveControllerTheme {
                ValveControllerApp(viewModel = viewModel)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}

object AppColors {
    val Primary = Color(0xFF2260D3)
    val Secondary = Color(0xFFBC3843)
    val Background = Color(0xFFF8FAFC)
    val Surface = Color.White
    val TextPrimary = Color(0xFF1E293B)
    val TextSecondary = Color(0xFF64748B)
    val Warning = Color(0xFFF59E0B)
}

@Composable
fun ValveControllerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.Primary,
            secondary = AppColors.Secondary,
            background = AppColors.Background,
            surface = AppColors.Surface
        ),
        content = content
    )
}

@Composable
fun ValveControllerApp(viewModel: ValveViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppTitle()
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.height(24.dp))

        StatusIndicator(
            uiState = uiState,
            onDismissAlert = { viewModel.handleAction(ValveViewModel.ValveAction.DismissAlert) }
        )

        Spacer(modifier = Modifier.height(32.dp))

        ControlButtons(
            uiState = uiState,
            onOpenClicked = { viewModel.handleAction(ValveViewModel.ValveAction.OpenValve) },
            onCloseClicked = { viewModel.handleAction(ValveViewModel.ValveAction.CloseValve) }
        )
    }
}

@Composable
fun AppTitle() {
    Text(
        text = "Water Valve Controller",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = AppColors.TextPrimary,
        textAlign = TextAlign.Center
    )
}

@Composable
fun StatusIndicator(uiState: ValveViewModel.UiState, onDismissAlert: () -> Unit = {}) {
    val (statusText, statusColor, showDismiss) = when (uiState) {
        is ValveViewModel.UiState.Ready -> Triple("Ready", AppColors.TextSecondary, false)
        is ValveViewModel.UiState.OpeningValve -> Triple("Opening valve...", AppColors.TextSecondary, false)
        is ValveViewModel.UiState.ClosingValve -> Triple("Closing valve...", AppColors.TextSecondary, false)
        is ValveViewModel.UiState.Success -> Triple(uiState.message, AppColors.TextSecondary, false)
        is ValveViewModel.UiState.Error -> Triple(uiState.message, AppColors.TextSecondary, false)
        is ValveViewModel.UiState.SensorAlert -> {
            val time = java.time.Instant.ofEpochMilli(uiState.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            Triple("⚠️ Water detected $time", AppColors.Warning, true)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (showDismiss) 72.dp else 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = statusText,
                fontSize = 16.sp,
                color = statusColor,
                textAlign = TextAlign.Center
            )

            if (showDismiss) {
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onDismissAlert) {
                    Text("Dismiss", fontSize = 12.sp, color = AppColors.TextSecondary)
                }
            }
        }
    }
}


@Composable
fun ControlButtons(
    uiState: ValveViewModel.UiState,
    onOpenClicked: () -> Unit,
    onCloseClicked: () -> Unit
) {
    val buttonsEnabled = when (uiState) {
        is ValveViewModel.UiState.OpeningValve,
        is ValveViewModel.UiState.ClosingValve -> false
        else -> true
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ModernButton(
            text = "OPEN VALVE",
            backgroundColor = AppColors.Primary,
            enabled = buttonsEnabled,
            onClick = onOpenClicked,
            modifier = Modifier.weight(1f)
        )

        ModernButton(
            text = "CLOSE VALVE",
            backgroundColor = AppColors.Secondary,
            enabled = buttonsEnabled,
            onClick = onCloseClicked,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun ModernButton(
    text: String,
    backgroundColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = Color.White,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.7f)
        )
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}