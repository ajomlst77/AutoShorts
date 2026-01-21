package com.autoshorts.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private var pickedVideoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pickVideoLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            pickedVideoUri = uri
        }

        setContent {
            MaterialTheme {
                var status by remember { mutableStateOf("Belum pilih video") }
                var uiVideoUri by remember { mutableStateOf<Uri?>(null) }

                // sync state dari launcher
                LaunchedEffect(pickedVideoUri) {
                    uiVideoUri = pickedVideoUri
                    if (uiVideoUri != null) status = "Video dipilih ✔"
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "AutoShorts MVP",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Button(
                        onClick = {
                            pickVideoLauncher.launch("video/*")
                        }
                    ) {
                        Text("Import Video")
                    }

                    Text(status)

                    Button(
                        onClick = {
                            val videoUri = uiVideoUri
                            if (videoUri == null) {
                                status = "Pilih video dulu!"
                                return@Button
                            }

                            try {
                                val result = Exporter.export(
                                    context = this@MainActivity,
                                    videoUri = videoUri,
                                    transcriptText = "Ini transcript contoh\nBaris kedua\nBaris ketiga",
                                    metaText = "Score: 87\nHook: Emosional\nStyle: Alex Hormozi",
                                    clipName = "autos"
                                )
                                status = "Export selesai ✔\nFolder:\n${result.folder.absolutePath}"
                            } catch (e: Exception) {
                                status = "Export gagal ❌\n${e.message}"
                            }
                        },
                        enabled = (uiVideoUri != null)
                    ) {
                        Text("Export (sementara)")
                    }
                }
            }
        }
    }
}
