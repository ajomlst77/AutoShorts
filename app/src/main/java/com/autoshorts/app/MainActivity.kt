package com.autoshorts.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

            var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

            val pickVideoLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri ->
                    selectedVideoUri = uri
                }

            Scaffold { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Button(
                        onClick = {
                            pickVideoLauncher.launch("video/*")
                        },
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Import Video")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            val meta = VideoMeta(
                                videoUri = selectedVideoUri?.toString() ?: "",
                                width = 0,
                                height = 0,
                                rotation = 0,
                                mimeType = "",
                                fileSizeBytes = 0L
                            )

                            val result = Exporter.export(meta)
                            println(result)
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        enabled = selectedVideoUri != null
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}
