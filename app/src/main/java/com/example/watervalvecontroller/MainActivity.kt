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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

object AppColorsLight {
    val Primary = Color(0xFF002D72)
    val Secondary = Color(0xFFFF9E1B)
    val Background = Color.White
    val Surface = Color(0xFFF5F5F5)
    val TextPrimary = Color(0xFF31261D)
    val TextSecondary = Color(0xFF666666)
    val Warning = Color(0xFFCF4520)
}

object AppColorsDark {
    val Primary = Color(0xFF68ACE5)
    val Secondary = Color(0xFFF1C400)
    val Background = Color(0xFF121212)
    val Surface = Color(0xFF1E1E1E)
    val TextPrimary = Color.White
    val TextSecondary = Color(0xFFB3B3B3)
    val Warning = Color(0xFFF56600)
}

@Composable
fun ValveControllerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        }
        darkTheme -> darkColorScheme(
            primary = AppColorsDark.Primary,
            secondary = AppColorsDark.Secondary,
            background = AppColorsDark.Background,
            surface = AppColorsDark.Surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = AppColorsDark.TextPrimary,
            onSurface = AppColorsDark.TextSecondary
        )
        else -> lightColorScheme(
            primary = AppColorsLight.Primary,
            secondary = AppColorsLight.Secondary,
            background = AppColorsLight.Background,
            surface = AppColorsLight.Surface,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = AppColorsLight.TextPrimary,
            onSurface = AppColorsLight.TextSecondary
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun ValveControllerApp(viewModel: ValveViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
}

@Composable
fun StatusIndicator(uiState: ValveViewModel.UiState, onDismissAlert: () -> Unit = {}) {
    val warningColor = if (isSystemInDarkTheme()) AppColorsDark.Warning else AppColorsLight.Warning

    val (statusText, statusColor, showDismiss) = when (uiState) {
        is ValveViewModel.UiState.Ready -> Triple("Ready", MaterialTheme.colorScheme.onSurface, false)
        is ValveViewModel.UiState.OpeningValve -> Triple("Opening valve...", MaterialTheme.colorScheme.onSurface, false)
        is ValveViewModel.UiState.ClosingValve -> Triple("Closing valve...", MaterialTheme.colorScheme.onSurface, false)
        is ValveViewModel.UiState.Success -> Triple(uiState.message, MaterialTheme.colorScheme.onSurface, false)
        is ValveViewModel.UiState.Error -> Triple(uiState.message, MaterialTheme.colorScheme.onSurface, false)
        is ValveViewModel.UiState.SensorAlert -> {
            val time = java.time.Instant.ofEpochMilli(uiState.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            Triple("⚠️ Water detected $time", warningColor, true)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (showDismiss) 72.dp else 48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    Text("Dismiss", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
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
            backgroundColor = MaterialTheme.colorScheme.primary,
            enabled = buttonsEnabled,
            onClick = onOpenClicked,
            modifier = Modifier.weight(1f)
        )

        ModernButton(
            text = "CLOSE VALVE",
            backgroundColor = MaterialTheme.colorScheme.secondary,
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