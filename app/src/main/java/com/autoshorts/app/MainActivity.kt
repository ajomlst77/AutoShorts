package com.autoshorts.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    var selectedUri by remember { mutableStateOf<Uri?>(null) }
                    var exportedText by remember { mutableStateOf("") }

                    // Launcher untuk pilih video dari file manager/galeri
                    val pickVideoLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        selectedUri = uri
                        exportedText = "" // reset hasil export saat pilih video baru
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AutoShorts",
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // INFO URI terpilih
                        Text(
                            text = if (selectedUri != null) {
                                "Video dipilih:\n${selectedUri.toString()}"
                            } else {
                                "Belum ada video yang dipilih"
                            },
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Tombol Import Video
                        Button(
                            onClick = { pickVideoLauncher.launch("video/*") },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Import Video")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Tombol Export
                        Button(
                            onClick = {
                                val uri = selectedUri
                                if (uri == null) {
                                    exportedText = "Pilih video dulu (Import Video)."
                                    return@Button
                                }

                                // Buat meta sederhana (kalau VideoAnalyzer kamu sudah ada,
                                // kamu bisa ganti bagian ini supaya lebih akurat)
                                val meta = VideoMeta(
                                    uri = uri.toString()
                                    // sisanya ada default, jadi aman dari error "No value passed..."
                                )

                                // Panggil Exporter TANPA context
                                exportedText = Exporter.export(meta)

                                // Copy ke clipboard biar gampang
                                copyToClipboard(this@MainActivity, "export_result", exportedText)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Export")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (exportedText.isNotBlank()) {
                            Text(
                                text = "Hasil Export (sudah di-copy ke clipboard):\n\n$exportedText",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Tombol Share hasil export
                            Button(
                                onClick = {
                                    shareText(this@MainActivity, exportedText)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Share Hasil Export")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun copyToClipboard(context: Context, label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    private fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Share"))
    }
}
