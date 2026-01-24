package com.autoshorts.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
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
                            val meta = VideoMeta(
                                videoUri = "",
                                width = 0,
                                height = 0,
                                rotation = 0,
                                mimeType = "",
                                fileSizeBytes = 0L
                            )

                            Exporter.export(meta)
                        }
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}
