package com.autoshorts.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
            AutoShortsUI()
        }
    }
}

@Composable
fun AutoShortsUI() {

    val context = LocalContext.current

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("Belum pilih video") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            status = "Video berhasil dipilih ✔"
        } else {
            status = "Pemilihan video dibatalkan"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {

        Text(
            text = "AutoShorts MVP",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            picker.launch("video/*")
        }) {
            Text("Import Video")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(status)

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            enabled = videoUri != null,
            onClick = {
                val transcriptText = "Transcript sementara..."
                val metaText = "Judul: Contoh\nHook: Emosional\nStyle: Alex Hormozi"

                Exporter.export(
                    context = context,
                    sourceVideoUri = videoUri!!,
                    transcriptText = transcriptText,
                    metaText = metaText
                )

                status = "Export selesai ✔\nCek folder Movies/AutoShorts"
            }
        ) {
            Text("Export")
        }
    }
}
