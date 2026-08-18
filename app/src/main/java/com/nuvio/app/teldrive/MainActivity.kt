package com.nuvio.app.teldrive

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections

// Color Palette Definition (Nord-inspired Slate Dark Theme)
private val ThemeBackground = Color(0xFF121214) // Charcoal Black
private val ThemeSurface = Color(0xFF1E1E24) // Deep Slate Grey
private val ThemeSurfaceVariant = Color(0xFF2E2E38) // Medium Slate Grey
private val ThemePrimary = Color(0xFF81A1C1) // Nord Ice Blue
private val ThemeSecondary = Color(0xFF88C0D0) // Ice Teal
private val ThemeAccentGreen = Color(0xFF2AABEE) // Telegram Blue
private val ThemeAccentRed = Color(0xFFBF616A) // Warning Terracotta Red
private val ThemeTextPrimary = Color(0xFFECEFF4) // Off-white
private val ThemeTextSecondary = Color(0xFFD8DEE9) // Light grey

fun getLocalIpAddress(): String? {
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs = Collections.list(intf.inetAddresses)
            for (addr in addrs) {
                if (!addr.isLoopbackAddress) {
                    val sAddr = addr.hostAddress ?: continue
                    val isIPv4 = sAddr.indexOf(':') < 0
                    if (isIPv4) {
                        return sAddr
                    }
                }
            }
        }
    } catch (ex: Exception) {
        // ignore
    }
    return null
}

suspend fun fetchLocalToken(port: Int): String? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL("http://127.0.0.1:$port/api/local/token")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

// Thread-safe log collector to receive callback log streams from the native process
object TelDriveLogCollector {
    val logLines = mutableStateListOf<String>()

    init {
        TelDriveManager.logListener = { line ->
            if (logLines.size > 1000) {
                logLines.removeAt(0)
            }
            val cleanLine = line.replace("\u001B\\[[0-9;]*[a-zA-Z]".toRegex(), "")
            logLines.add(cleanLine)
        }
    }

    fun clear() {
        logLines.clear()
    }
}

enum class ActiveScreen {
    Setup,
    Dashboard
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TelDriveConfigRepository.initialize(this)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }

        setContent {
            val isRunning by TelDriveManager.runningState.collectAsState()
            // Apply custom dark theme colors
            val customColorScheme = darkColorScheme(
                background = ThemeBackground,
                surface = ThemeSurface,
                surfaceVariant = ThemeSurfaceVariant,
                onSurface = ThemeTextPrimary,
                onSurfaceVariant = ThemeTextSecondary,
                primary = ThemePrimary,
                secondary = ThemeSecondary,
                onBackground = ThemeTextPrimary
            )

            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.statusBarColor = ThemeSurface.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                }
            }

            MaterialTheme(colorScheme = customColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ThemeBackground
                ) {
                    var currentScreen by remember {
                        mutableStateOf(
                            if (TelDriveConfigRepository.isConfigured()) ActiveScreen.Dashboard else ActiveScreen.Setup
                        )
                    }

                    when (currentScreen) {
                        ActiveScreen.Setup -> SetupScreen(
                            isRunning = isRunning,
                            onNavigateBack = { currentScreen = ActiveScreen.Dashboard }
                        )
                        ActiveScreen.Dashboard -> DashboardScreen(
                            onNavigateToSetup = { currentScreen = ActiveScreen.Setup }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    isRunning: Boolean,
    onNavigateBack: () -> Unit
) {
    var dsn by remember { mutableStateOf(TelDriveConfigRepository.supabaseDsn) }
    var jwtSecret by remember { mutableStateOf(TelDriveConfigRepository.jwtSecret) }
    var port by remember { mutableStateOf(TelDriveConfigRepository.port.toString()) }
    var appId by remember { mutableStateOf(TelDriveConfigRepository.tgAppId) }
    var appHash by remember { mutableStateOf(TelDriveConfigRepository.tgAppHash) }
    var encryptionKey by remember { mutableStateOf(TelDriveConfigRepository.tgUploadsEncryptionKey) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TelDrive Configuration Wizard", fontWeight = FontWeight.Bold, color = ThemeTextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeSurface,
                )
            )
        },
        containerColor = ThemeBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isRunning) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ThemeAccentRed)
                    ) {
                        Text(
                            text = "Configuration is locked while the server is active. Stop the server on the dashboard to change settings.",
                            fontWeight = FontWeight.Bold,
                            color = ThemeTextPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Configure your Supabase Database and Telegram credentials to initialize the gateway backend daemon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ThemeTextSecondary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ThemeSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Database Settings", fontWeight = FontWeight.Bold, color = ThemePrimary)
                        
                        OutlinedTextField(
                            value = dsn,
                            onValueChange = { dsn = it },
                            label = { Text("Supabase PostgreSQL DSN", color = ThemeTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isRunning,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ThemeTextPrimary,
                                unfocusedTextColor = ThemeTextPrimary,
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = ThemeSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = jwtSecret,
                            onValueChange = { jwtSecret = it },
                            label = { Text("JWT Secret Key", color = ThemeTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isRunning,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ThemeTextPrimary,
                                unfocusedTextColor = ThemeTextPrimary,
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = ThemeSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it.filter { c -> c.isDigit() } },
                            label = { Text("Gateway Daemon Server Port", color = ThemeTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            enabled = !isRunning,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ThemeTextPrimary,
                                unfocusedTextColor = ThemeTextPrimary,
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = ThemeSurfaceVariant
                            )
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ThemeSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Telegram API Credentials", fontWeight = FontWeight.Bold, color = ThemePrimary)

                        OutlinedTextField(
                            value = appId,
                            onValueChange = { appId = it },
                            label = { Text("Telegram App ID", color = ThemeTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isRunning,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ThemeTextPrimary,
                                unfocusedTextColor = ThemeTextPrimary,
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = ThemeSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = appHash,
                            onValueChange = { appHash = it },
                            label = { Text("Telegram App Hash", color = ThemeTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isRunning,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ThemeTextPrimary,
                                unfocusedTextColor = ThemeTextPrimary,
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = ThemeSurfaceVariant
                            )
                        )

                        OutlinedTextField(
                            value = encryptionKey,
                            onValueChange = { encryptionKey = it },
                            label = { Text("Uploads Encryption Key", color = ThemeTextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !isRunning,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ThemeTextPrimary,
                                unfocusedTextColor = ThemeTextPrimary,
                                focusedBorderColor = ThemePrimary,
                                unfocusedBorderColor = ThemeSurfaceVariant
                            )
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = ThemeSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Rclone Integration", fontWeight = FontWeight.Bold, color = ThemePrimary)
                        Text(
                            text = "To access your files from Rclone, copy your local session auth token.",
                            style = MaterialTheme.typography.bodySmall,
                            color = ThemeTextSecondary
                        )
                        Button(
                            onClick = {
                                if (isRunning) {
                                    scope.launch {
                                        val token = fetchLocalToken(TelDriveConfigRepository.port)
                                        if (token != null) {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Rclone Auth Token", token)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "Rclone Auth Token copied!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to fetch token. Ensure you have completed Telegram setup.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Server is not running. Please start the server first.", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRunning) ThemeSecondary else ThemeSurfaceVariant,
                                contentColor = ThemeTextPrimary
                            )
                        ) {
                            Text("Copy Rclone Auth Token", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (!isRunning) {
                            TelDriveConfigRepository.supabaseDsn = dsn
                            TelDriveConfigRepository.jwtSecret = jwtSecret
                            TelDriveConfigRepository.port = port.toIntOrNull() ?: 8080
                            TelDriveConfigRepository.tgAppId = appId
                            TelDriveConfigRepository.tgAppHash = appHash
                            TelDriveConfigRepository.tgUploadsEncryptionKey = encryptionKey
                        }
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isRunning || (dsn.isNotBlank() && jwtSecret.isNotBlank() && appId.isNotBlank() && appHash.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(containerColor = ThemePrimary)
                ) {
                    Text(
                        text = if (isRunning) "Back to Dashboard" else "Save Config & Continue",
                        fontWeight = FontWeight.Bold,
                        color = ThemeTextPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToSetup: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isRunning by TelDriveManager.runningState.collectAsState()
    val listState = rememberLazyListState()
    val logs = TelDriveLogCollector.logLines

    // Auto-scroll to bottom of console when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Teldrive", fontWeight = FontWeight.Bold, color = ThemeTextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ThemeSurface,
                )
            )
        },
        containerColor = ThemeBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Engine Control Switch Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ThemeSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRunning) "Server status: RUNNING" else "Server status: STOPPED",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isRunning) ThemeAccentGreen else ThemeAccentRed
                        )
                        if (isRunning) {
                            val ip = getLocalIpAddress()
                            Text(
                                text = "Local: http://localhost:${TelDriveConfigRepository.port}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemeTextSecondary
                            )
                            if (ip != null) {
                                Text(
                                    text = "Network: http://$ip:${TelDriveConfigRepository.port}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ThemeSecondary
                                )
                            }
                        } else {
                            Text(
                                text = "Activate server to run administration console",
                                style = MaterialTheme.typography.bodySmall,
                                color = ThemeTextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = isRunning,
                        onCheckedChange = { checked ->
                            if (checked) {
                                TelDriveService.startService(
                                    context = context,
                                    supabaseDsn = TelDriveConfigRepository.supabaseDsn,
                                    jwtSecret = TelDriveConfigRepository.jwtSecret,
                                    port = TelDriveConfigRepository.port,
                                    tgAppId = TelDriveConfigRepository.tgAppId,
                                    tgAppHash = TelDriveConfigRepository.tgAppHash,
                                    tgUploadsEncryptionKey = TelDriveConfigRepository.tgUploadsEncryptionKey
                                )
                            } else {
                                TelDriveService.stopService(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ThemeAccentGreen,
                            checkedTrackColor = ThemeAccentGreen.copy(alpha = 0.5f),
                            uncheckedThumbColor = ThemeTextSecondary,
                            uncheckedTrackColor = ThemeSurfaceVariant
                        )
                    )
                }
            }

            // Navigation Actions (Sleek full-width button to open browser console)
            Button(
                onClick = {
                    val url = "http://localhost:${TelDriveConfigRepository.port}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemePrimary,
                    contentColor = ThemeTextPrimary
                )
            ) {
                Text("Open TelDrive Webapp", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Logs Console Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daemon Console Output", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = ThemeTextPrimary)
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("TelDrive Logs", logs.joinToString("\n"))
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Logs copied!", Toast.LENGTH_SHORT).show()
                        },
                        enabled = logs.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeSurfaceVariant, contentColor = ThemeTextPrimary)
                    ) {
                        Text("Copy", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { TelDriveLogCollector.clear() },
                        enabled = logs.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = ThemeSurfaceVariant, contentColor = ThemeTextPrimary)
                    ) {
                        Text("Clear", fontSize = 11.sp)
                    }
                }
            }

            // Dark Terminal Console
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0C0F11))
                    .padding(8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { line ->
                        Text(
                            text = line,
                            color = Color(0xFF2AABEE),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Settings edit button at bottom
            Button(
                onClick = onNavigateToSetup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ThemeSurface,
                    contentColor = ThemeTextPrimary
                )
            ) {
                Text(if (isRunning) "View Configuration & Token" else "Edit Settings Configuration")
            }
        }
    }
}
