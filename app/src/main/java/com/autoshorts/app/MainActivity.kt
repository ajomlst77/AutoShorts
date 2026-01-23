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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {

            var selectedVideo by remember { mutableStateOf<Uri?>(null) }
            var status by remember { mutableStateOf("Belum ada video") }

            // 🔹 LAUNCHER IMPORT VIDEO
            val videoPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    selectedVideo = uri
                    status = "Video dipilih"
                }
            }

            Scaffold { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Text("AutoShorts")

                    Spacer(modifier = Modifier.height(20.dp))

                    // ✅ TOMBOL IMPORT
                    Button(
                        onClick = {
                            videoPicker.launch("video/*")
                        }
                    ) {
                        Text("Import Video")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(status)

                    Spacer(modifier = Modifier.height(32.dp))

                    // ✅ TOMBOL EXPORT
                    Button(
                        enabled = selectedVideo != null,
                        onClick = {
                            val meta = VideoMeta(
                                videoUri = selectedVideo!!
                            )

                            Exporter.export(this@MainActivity, meta)
                            status = "Export selesai"
                        }
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}
