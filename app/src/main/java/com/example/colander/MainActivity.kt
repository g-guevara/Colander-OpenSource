package com.example.colander

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.colander.ui.theme.ColanderTheme

class MainActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            checkUsageStatsPermission()
        } else {
            showToast("Se necesita permiso de overlay para mostrar bloqueos", isError = true)
        }
    }

    private val accessibilityPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (hasAccessibilityPermission()) {
            startDetectionService()
        } else {
            showToast("Se necesita permiso de accesibilidad para detectar elementos de Instagram", isError = true)
        }
    }

    private val usageStatsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (hasUsageStatsPermission()) {
            checkAccessibilityPermission()
        } else {
            showToast("Se necesita permiso de estadísticas de uso para detectar Instagram", isError = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("cube_settings", Context.MODE_PRIVATE)

        enableEdgeToEdge()
        setContent {
            ColanderTheme {
                MainScreen(
                    onStartDetection = { checkPermissions() },
                    onStopDetection = { stopDetectionService() }
                )
            }
        }
    }

    private fun checkPermissions() {
        when {
            !Settings.canDrawOverlays(this) -> {
                showToast("Configurando permiso de overlay...")
                requestOverlayPermission()
            }
            !hasUsageStatsPermission() -> {
                showToast("Configurando permiso de estadísticas de uso...")
                checkUsageStatsPermission()
            }
            !hasAccessibilityPermission() -> {
                showToast("Configurando permiso de accesibilidad...")
                checkAccessibilityPermission()
            }
            else -> {
                startDetectionService()
            }
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun checkUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            usageStatsPermissionLauncher.launch(intent)
        } else {
            checkAccessibilityPermission()
        }
    }

    private fun checkAccessibilityPermission() {
        if (!hasAccessibilityPermission()) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            accessibilityPermissionLauncher.launch(intent)
        } else {
            startDetectionService()
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasAccessibilityPermission(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val serviceName = colonSplitter.next()
            if (serviceName.contains("com.example.colander") &&
                serviceName.contains("InstagramAccessibilityService")) {
                return true
            }
        }
        return false
    }

    private fun startDetectionService() {
        try {
            val serviceIntent = Intent(this, AppDetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            prefs.edit().putBoolean("service_was_active", true).apply()

            showToast("✅ Bloqueador de Instagram activado correctamente")
        } catch (e: Exception) {
            showToast("❌ Error iniciando el servicio: ${e.message}", isError = true)
        }
    }

    private fun stopDetectionService() {
        try {
            val serviceIntent = Intent(this, AppDetectionService::class.java)
            stopService(serviceIntent)

            prefs.edit().putBoolean("service_was_active", false).apply()

            showToast("⏹️ Bloqueador de Instagram desactivado")
        } catch (e: Exception) {
            showToast("❌ Error deteniendo el servicio: ${e.message}", isError = true)
        }
    }

    private fun showToast(message: String, isError: Boolean = false) {
        Toast.makeText(
            this,
            message,
            if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    onStartDetection: () -> Unit,
    onStopDetection: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cube_settings", Context.MODE_PRIVATE) }
    val scrollState = rememberScrollState()

    // Estados de configuración
    var topMarginFeed by remember { mutableStateOf(prefs.getInt("top_margin_feed", 100).toString()) }
    var topMarginSearch by remember { mutableStateOf(prefs.getInt("top_margin_search", 150).toString()) }
    var bottomMargin by remember { mutableStateOf(prefs.getInt("bottom_margin", 100).toString()) }
    var isServiceActive by remember { mutableStateOf(prefs.getBoolean("service_was_active", false)) }
    var showSettings by remember { mutableStateOf(false) }

    // Colores del diseño
    val backgroundColor = Color(0xFFC7C6BF)
    val circleColor = Color.Black
    val circleSecondaryColor = Color(0xFFADADA4) // Color específico para círculos no negros
    val textColor = Color.Black

    // Función para guardar configuración
    fun saveOverlaySettings() {
        val topFeed = topMarginFeed.toIntOrNull() ?: 100
        val topSearch = topMarginSearch.toIntOrNull() ?: 150
        val bottom = bottomMargin.toIntOrNull() ?: 100

        val validTopFeed = topFeed.coerceIn(0, 500)
        val validTopSearch = topSearch.coerceIn(0, 500)
        val validBottom = bottom.coerceIn(0, 500)

        prefs.edit()
            .putInt("top_margin_feed", validTopFeed)
            .putInt("top_margin_search", validTopSearch)
            .putInt("bottom_margin", validBottom)
            .apply()

        topMarginFeed = validTopFeed.toString()
        topMarginSearch = validTopSearch.toString()
        bottomMargin = validBottom.toString()

        val intent = Intent("com.example.colander.OVERLAY_SETTINGS_UPDATE")
        intent.putExtra("top_margin_feed", validTopFeed)
        intent.putExtra("top_margin_search", validTopSearch)
        intent.putExtra("bottom_margin", validBottom)
        context.sendBroadcast(intent)

        Toast.makeText(
            context,
            "Configuración guardada",
            Toast.LENGTH_SHORT
        ).show()
    }

    // NUEVO: Manejo de window insets para respetar status bar y navigation bar
    Scaffold(
        containerColor = backgroundColor,
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Esto respeta los insets del sistema automáticamente
                    .background(backgroundColor)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Grid de círculos 5x6
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Patrón específico como en la imagen
                        val pattern = listOf(
                            listOf(false, true, true, false, false, false),
                            listOf(false, false, false, false, false, false),
                            listOf(false, false, false, false, false, true),
                            listOf(true, true, false, false, false, false),
                            listOf(false, false, true, true, true, false)
                        )

                        pattern.forEach { row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { isBlack ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isBlack) circleColor else circleSecondaryColor
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Línea separadora después de los círculos
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = textColor.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Título "colander"
                    Text(
                        text = "colander",
                        fontSize = 24.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Línea separadora antes de la sección Instagram
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = textColor.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sección Instagram
                    Text(
                        text = "Instagram",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = textColor,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dimensiones
                    Text(
                        text = "Dimensions",
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textColor,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Línea separadora antes de las dimensiones
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 0.5.dp,
                        color = textColor.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Stories, Search Page, Navtab con valores
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Stories",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                            Text(
                                text = "400",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Search Page",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                            Text(
                                text = topMarginSearch,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Navtab",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                            Text(
                                text = bottomMargin,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Línea separadora antes de los controles
                    Divider(
                        modifier = Modifier.fillMaxWidth(),
                        thickness = 1.dp,
                        color = textColor.copy(alpha = 0.2f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Controles del servicio
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                onStartDetection()
                                isServiceActive = true
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isServiceActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = textColor,
                                contentColor = backgroundColor
                            )
                        ) {
                            Text(
                                "Start",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = {
                                onStopDetection()
                                isServiceActive = false
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isServiceActive,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = textColor,
                                contentColor = backgroundColor
                            )
                        ) {
                            Text(
                                "Stop",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Panel de configuración (mostrar solo si showSettings es true)
                    if (showSettings) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Línea separadora antes del panel de configuración
                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = textColor.copy(alpha = 0.3f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = backgroundColor.copy(alpha = 0.8f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Settings",
                                    fontSize = 16.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Línea separadora dentro del panel Settings
                                Divider(
                                    modifier = Modifier.fillMaxWidth(),
                                    thickness = 0.5.dp,
                                    color = textColor.copy(alpha = 0.1f)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Feed margin
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Feed Top",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor
                                    )
                                    OutlinedTextField(
                                        value = topMarginFeed,
                                        onValueChange = { topMarginFeed = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(80.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Search margin
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Search Top",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor
                                    )
                                    OutlinedTextField(
                                        value = topMarginSearch,
                                        onValueChange = { topMarginSearch = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(80.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Bottom margin
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Bottom",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor
                                    )
                                    OutlinedTextField(
                                        value = bottomMargin,
                                        onValueChange = { bottomMargin = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(80.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Línea separadora antes del botón Save
                                Divider(
                                    modifier = Modifier.fillMaxWidth(),
                                    thickness = 0.5.dp,
                                    color = textColor.copy(alpha = 0.1f)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { saveOverlaySettings() },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = textColor,
                                        contentColor = backgroundColor
                                    )
                                ) {
                                    Text(
                                        "Save",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // Espacio adicional para evitar que el botón Settings se superponga
                    Spacer(modifier = Modifier.height(80.dp))
                }

                // Botón Settings en la esquina inferior derecha
                Button(
                    onClick = { showSettings = !showSettings },
                    modifier = Modifier
                        .align(Alignment.BottomEnd).padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = textColor,
                        contentColor = backgroundColor
                    )
                ) {
                    Text(
                        "Settings>",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    )
}