package com.autoshorts.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private var selectedVideoUri: Uri? = null

    private val pickVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedVideoUri = uri
                Toast.makeText(this, "Video dipilih ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Batal pilih video", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = "AutoShorts",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                pickVideoLauncher.launch("video/*")
                            }
                        ) {
                            Text("Import Video")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                val uri = selectedVideoUri
                                if (uri == null) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Import video dulu",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                val meta = VideoAnalyzer.analyze(this@MainActivity, uri)
                                val result = Exporter.export(meta)

                                Toast.makeText(
                                    this@MainActivity,
                                    result,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        ) {
                            Text("Export")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (selectedVideoUri == null)
                                "Belum ada video"
                            else
                                "Video siap diproses ✅"
                        )
                    }
                }
            }
        }
    }
}
