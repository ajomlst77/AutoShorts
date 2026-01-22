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

            MaterialTheme {

                // ====== STATE ======
                var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }

                // Sementara: transcript & meta masih dummy (nanti step berikutnya kita isi dari AI)
                var transcriptText by remember {
                    mutableStateOf(
                        """
1
00:00:00,000 --> 00:00:02,000
INI CONTOH CAPTION SRT

2
00:00:02,000 --> 00:00:04,000
NANTI DIGANTI HASIL AI
""".trimIndent()
                    )
                }

                var metaText by remember {
                    mutableStateOf(
                        """
title=AutoShorts MVP Export
hook=Ini contoh meta
hashtag=#shorts #viral
score=0.0
""".trimIndent()
                    )
                }

                var statusText by remember { mutableStateOf("") }

                // ====== PICKER (IMPORT VIDEO) ======
                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        // simpan uri hasil pilih
                        selectedVideoUri = uri

                        // opsional: persist permission (biar tetap bisa akses setelah restart)
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                IntentFlags.READ
                            )
                        } catch (_: Exception) {
                            // kalau gagal, gapapa (tetap biasanya bisa dipakai)
                        }

                        statusText = "Video terpilih ✅\nSekarang tekan Export"
                    } else {
                        statusText = "Batal pilih video."
                    }
                }

                // ====== UI ======
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AutoShorts MVP",
                            style = MaterialTheme.typography.headlineLarge
                        )

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                // video types
                                importLauncher.launch(arrayOf("video/*"))
                            }
                        ) {
                            Text("Import Video")
                        }

                        Spacer(Modifier.height(18.dp))

                        if (statusText.isNotBlank()) {
                            Text(statusText)
                        }

                        Spacer(Modifier.height(24.dp))

                        Button(
                            onClick = {
                                val uri = selectedVideoUri
                                if (uri == null) {
                                    statusText = "❌ Belum import video.\nKlik Import Video dulu."
                                    return@Button
                                }

                                // ====== EXPORT CALL ======
                                val result = Exporter.export(
                                    context = this@MainActivity,
                                    inputVideoUri = uri,
                                    transcriptText = transcriptText,
                                    metaText = metaText
                                )

                                statusText = result.message
                            }
                        ) {
                            Text("Export")
                        }

                        Spacer(Modifier.height(10.dp))
                        Text("Hasil export: Movies/AutoShorts/... (lihat File Manager)")
                    }
                }
            }
        }
    }
}

/**
 * Helper supaya tidak import Intent secara full.
 * (Karena di Compose kadang import Intent bikin bentrok di editor HP)
 */
private object IntentFlags {
    const val READ = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
}
