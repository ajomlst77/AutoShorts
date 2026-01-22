package com.autoshorts.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var selectedUri by remember { mutableStateOf<Uri?>(null) }
            var status by remember { mutableStateOf("") }

            val pickVideoLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia(),
                onResult = { uri ->
                    selectedUri = uri
                    status = if (uri != null) "Video dipilih ✅" else "Batal memilih video"
                }
            )

            MainScreen(
                selectedVideo = selectedUri,
                statusText = status,
                onPickVideo = {
                    pickVideoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                },
                onExport = {
                    // kalau Exporter.export() butuh input video, nanti kita sambungkan dari selectedUri
                    val result = Exporter.export()
                    status = result
                }
            )
        }
    }
}

@Composable
private fun MainScreen(
    selectedVideo: Uri?,
    statusText: String,
    onPickVideo: () -> Unit,
    onExport: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("AutoShorts") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("AutoShorts", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onPickVideo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text("Import Video")
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onExport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedVideo != null
            ) {
                Text("Export")
            }

            Spacer(Modifier.height(12.dp))

            if (selectedVideo != null) {
                Text(
                    text = "Dipilih: ${selectedVideo}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (statusText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(statusText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
