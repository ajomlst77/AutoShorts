package com.autoshorts.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppUI()
        }
    }
}

@Composable
fun AppUI() {

    val context = LocalContext.current

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Belum ada video") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        videoUri = uri
        status = if (uri != null) "Video dipilih ✔" else "Batal"
    }

    Column(modifier = Modifier.padding(24.dp)) {

        Text("AutoShorts MVP", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            picker.launch("video/*")
        }) {
            Text("Import Video")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(status)

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            if (videoUri != null) {
                Exporter.export(
                    context = context,
                    videoUri = videoUri!!,
                    transcript = "Ini transcript sementara",
                    meta = "Score: 87\nHook: Emosional\nStyle: Alex Hormozi"
                )
                status = "Export selesai ✔"
            }
        }) {
            Text("Export (sementara)")
        }
    }
}
