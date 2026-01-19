package com.autoshorts.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MaterialTheme {
        var videoUri by remember { mutableStateOf<Uri?>(null) }
        var status by remember { mutableStateOf("Klik Import Video") }

        val picker = rememberLauncherForActivityResult(
          contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
          videoUri = uri
          status = if (uri != null) "Video dipilih ✅" else "Batal memilih video"
        }

        Column(Modifier.fillMaxSize().padding(16.dp)) {
          Text("AutoShorts MVP", style = MaterialTheme.typography.titleLarge)
          Spacer(Modifier.height(12.dp))

          Button(onClick = {
            picker.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
          }) { Text("Import Video") }

          Spacer(Modifier.height(12.dp))
          Text(status)

          Spacer(Modifier.height(24.dp))
          Button(enabled = videoUri != null, onClick = {
            Button(enabled = videoUri != null, onClick = {
  status = Exporter.exportToDownloads(this@MainActivity, videoUri!!)
}) {
  Text("Export")
}

          }) {
            Text("Export (nanti)")
          }
        }
      }
    }
  }
}
