package com.example.ultrasonicreader

// Core Android and Compose libraries
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

/**
 * Activity that displays a step-by-step guide on how to connect
 * to the CC3200-based ultrasonic sensor system via Wi-Fi.
 */
class StepsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the content using Jetpack Compose
        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        // TopAppBar with back button and title
                        TopAppBar(
                            title = { Text("How to Connect", color = Color.White) },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) { // Close activity on back press
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            },
                            backgroundColor = MaterialTheme.colors.primary
                        )
                    }
                ) { padding ->
                    // Load instructional screen content
                    StepsScreen(Modifier.padding(padding))
                }
            }
        }
    }
}

/**
 * Composable function to show the setup instructions for users.
 */
@Composable
fun StepsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text("🔌 How to Connect to CC3200", fontSize = 22.sp, style = MaterialTheme.typography.h6)

        Spacer(modifier = Modifier.height(16.dp))

        // Step-by-step instructions for user to connect to CC3200
        Text(
            text = "1. Power on your CC3200 board.\n" +
                    "2. It will create a WiFi hotspot like `MySensorAP`.\n" +
                    "3. On your phone, connect to the hotspot via Wi-Fi settings.\n" +
                    "4. Open this app again while connected.\n" +
                    "5. The app reads distance from http://192.168.1.1/sensor.",
            style = MaterialTheme.typography.body1
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Additional note on expected data format from the board
        Text(
            text = "ℹ️ Ensure CC3200 runs in Access Point mode and responds with JSON like { \"distance\": 25 }.",
            style = MaterialTheme.typography.body2
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Developer's photo (optional branding/presentation enhancement)
        Image(
            painter = painterResource(id = R.drawable.developer_photo),
            contentDescription = "Developer Photo",
            modifier = Modifier
                .size(300.dp)
                .padding(bottom = 8.dp)
        )

        // Developer and supervisor credits
        Text(
            "Developed by Deep Savaliya",
            fontSize = 16.sp,
            color = Color.Black
        )

        Text(
            "Supervisor: Prof. Dr. Götz Winterfeldt",
            fontSize = 16.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(16.dp))

        // College logo for final branding
        Image(
            painter = painterResource(id = R.drawable.college_logo),
            contentDescription = "College Logo",
            modifier = Modifier
                .size(300.dp)
                .padding(top = 8.dp)
        )
    }
}
