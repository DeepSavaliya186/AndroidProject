package com.example.ultrasonicreader

// Core Android and Compose imports
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Ktor HTTP client with JSON serialization
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Vibration and intent handling
import android.os.VibrationEffect
import android.os.Vibrator
import android.content.Context
import android.content.Intent

// Extra UI elements
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

// Data model for JSON response
@Serializable
data class SensorData(val distance: Int)

/**
 * Main Activity for the DriveGuard app.
 * Displays live ultrasonic sensor distance and provides alerts (sound/vibration/UI)
 * when distance thresholds are reached.
 */
class MainActivity : ComponentActivity() {
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize alert sound player
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound)
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // Compose UI content
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            var isSoundEnabled by remember { mutableStateOf(true) }
            var isVibrationEnabled by remember { mutableStateOf(true) }
            var yellowPlayer: MediaPlayer? = null // Temporary player for warning beep

            val colors = if (isDarkMode) darkColors() else lightColors()

            MaterialTheme(colors = colors) {
                Scaffold(
                    topBar = {
                        // App Bar with toggles and navigation
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.app_logo),
                                        contentDescription = "App Logo",
                                        modifier = Modifier
                                            .size(90.dp)
                                            .padding(end = 8.dp),
                                        tint = Color.Unspecified
                                    )
                                    Text("DriveGuard", color = Color.White)
                                }
                            },
                            backgroundColor = MaterialTheme.colors.primary,
                            actions = {
                                // Sound toggle
                                IconToggleButton(checked = isSoundEnabled, onCheckedChange = { isSoundEnabled = it }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_volume),
                                        contentDescription = "Toggle Sound",
                                        tint = if (isSoundEnabled) Color.White else Color.Gray
                                    )
                                }
                                // Vibration toggle
                                IconToggleButton(checked = isVibrationEnabled, onCheckedChange = { isVibrationEnabled = it }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_vibration),
                                        contentDescription = "Toggle Vibration",
                                        tint = if (isVibrationEnabled) Color.White else Color.Gray
                                    )
                                }
                                // Theme toggle (light/dark)
                                IconButton(onClick = { isDarkMode = !isDarkMode }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_theme_toggle),
                                        contentDescription = "Toggle Theme",
                                        tint = Color.White
                                    )
                                }
                                // Navigate to help/tutorial screen
                                IconButton(onClick = {
                                    val intent = Intent(this@MainActivity, StepsActivity::class.java)
                                    startActivity(intent)
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.info),
                                        contentDescription = "How to Connect",
                                        tint = Color.White
                                    )
                                }
                            }
                        )
                    }
                ) { padding ->
                    // Main UI content for sensor visualization and alerts
                    OverParkingUI(
                        modifier = Modifier.padding(padding),
                        onDanger = {
                            // Handle danger state: red zone alert
                            if (isSoundEnabled && mediaPlayer?.isPlaying == false) {
                                mediaPlayer?.start()
                            }
                            if (isVibrationEnabled) {
                                vibrator.vibrate(
                                    VibrationEffect.createWaveform(longArrayOf(0, 300, 200, 300), -1)
                                )
                            }
                            // Stop any yellow beep sound
                            yellowPlayer?.stop()
                            yellowPlayer?.release()
                            yellowPlayer = null
                        },
                        onSafe = {
                            // Reset sound/vibration if in safe zone
                            if (mediaPlayer?.isPlaying == true) {
                                mediaPlayer?.pause()
                                mediaPlayer?.seekTo(0)
                            }
                            vibrator.cancel()
                            yellowPlayer?.stop()
                            yellowPlayer?.release()
                            yellowPlayer = null
                        },
                        onWarning = {
                            // Yellow zone: play short warning beep
                            if (isSoundEnabled && yellowPlayer == null) {
                                yellowPlayer = MediaPlayer.create(this@MainActivity, R.raw.warning_beep)
                                yellowPlayer?.setOnCompletionListener {
                                    it.release()
                                    yellowPlayer = null
                                }
                                yellowPlayer?.start()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        super.onDestroy()
    }
}

/**
 * Main Composable function to show proximity data and warnings.
 */
@Composable
fun OverParkingUI(
    modifier: Modifier = Modifier,
    onDanger: () -> Unit,
    onSafe: () -> Unit,
    onWarning: () -> Unit
) {
    val distance = remember { mutableStateOf(-1) }
    val status = remember { mutableStateOf("Waiting...") }

    val alertAlpha by remember {
        derivedStateOf {
            if (status.value == "DANGER") 1f else 0f
        }
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = alertAlpha,
        animationSpec = tween(durationMillis = 600),
        label = "AlertFade"
    )

    // HTTP client initialized once per composition
    val client = remember {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }

    // Periodic polling of ultrasonic sensor data
    LaunchedEffect(Unit) {
        while (true) {
            try {
                val response: String = client.get("http://192.168.1.1/sensor").body()
                val data = Json.decodeFromString<SensorData>(response)
                distance.value = data.distance

                when {
                    data.distance < 20 -> {
                        if (status.value != "DANGER") {
                            status.value = "DANGER"
                            onDanger()
                        }
                    }
                    data.distance in 20..29 -> {
                        if (status.value != "YELLOW") {
                            status.value = "YELLOW"
                            onWarning()
                        }
                    }
                    else -> {
                        if (status.value != "SAFE") {
                            onSafe()
                            status.value = "SAFE"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SENSOR", "Error: ${e.message}")
                distance.value = -1
                status.value = "No Connection"
            }
            delay(1000) // Poll every second
        }
    }

    // Main UI layout
    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        color = MaterialTheme.colors.background
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Car + colored proximity zones
            CarWithProximityZones(distance = distance.value)

            Spacer(modifier = Modifier.height(24.dp))

            // Distance + status card
            Card(
                elevation = 10.dp,
                shape = MaterialTheme.shapes.medium,
                backgroundColor = MaterialTheme.colors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Current Distance", style = MaterialTheme.typography.h6)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (distance.value >= 0) "${distance.value} cm" else "--",
                        style = MaterialTheme.typography.h3,
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Status: ${status.value}",
                        style = MaterialTheme.typography.subtitle1,
                        color = when (status.value) {
                            "DANGER" -> Color.Red
                            "SAFE" -> Color(0xFF2E7D32)
                            else -> Color.Gray
                        }
                    )

                    if (status.value == "DANGER") {
                        Spacer(modifier = Modifier.height(12.dp))
                        FlashingAlertText()
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Flashing alert bar
            if (status.value == "DANGER") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(alpha = animatedAlpha)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Red, Color.DarkGray)
                            ),
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        "🚨 WARNING: TOO CLOSE!",
                        color = Color.White,
                        style = MaterialTheme.typography.body1,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

/**
 * Pulsing red warning text during danger state.
 */
@Composable
fun FlashingAlertText() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        )
    )

    Text(
        text = "🚨 TOO CLOSE! STOP!",
        fontSize = 20.sp,
        color = Color.Red,
        modifier = Modifier.graphicsLayer { this.alpha = alpha }
    )
}

/**
 * Visual fan-based proximity visualization behind the car.
 * Color changes based on the current distance.
 */
@Composable
fun CarWithProximityZones(distance: Int) {
    // Determine color zone based on distance
    val zoneColor = when {
        distance in 0..9 -> Color.Red
        distance in 10..19 -> Color(0xFFFFA500) // Orange
        distance in 20..29 -> Color.Yellow
        distance >= 30 -> Color(0xFF00C853) // Green
        else -> Color.Gray
    }

    val animatedColor by animateColorAsState(
        targetValue = zoneColor,
        animationSpec = tween(durationMillis = 500),
        label = "ZoneColor"
    )

    // Draw fan-shaped proximity areas behind the car
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2f
            val startY = size.height * 0.45f
            val length = size.height * 0.4f
            val fanWidth = size.width * 0.4f

            // Outer layer
            drawPath(Path().apply {
                moveTo(centerX, startY)
                lineTo(centerX - fanWidth, startY + length)
                lineTo(centerX + fanWidth, startY + length)
                close()
            }, color = animatedColor.copy(alpha = 0.3f))

            // Middle layer
            drawPath(Path().apply {
                moveTo(centerX, startY)
                lineTo(centerX - fanWidth * 0.7f, startY + length * 0.7f)
                lineTo(centerX + fanWidth * 0.7f, startY + length * 0.7f)
                close()
            }, color = animatedColor.copy(alpha = 0.5f))

            // Inner layer
            drawPath(Path().apply {
                moveTo(centerX, startY)
                lineTo(centerX - fanWidth * 0.4f, startY + length * 0.4f)
                lineTo(centerX + fanWidth * 0.4f, startY + length * 0.4f)
                close()
            }, color = animatedColor.copy(alpha = 0.7f))
        }

        // Static car image overlay
        Image(
            painter = painterResource(id = R.drawable.car_top),
            contentDescription = "Car Top View",
            modifier = Modifier
                .size(180.dp)
                .align(Alignment.TopCenter)
        )
    }
}
