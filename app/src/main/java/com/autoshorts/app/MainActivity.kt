upackage com.autoshorts.app

import android.content.Intent
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
                val context = this

                var inputVideoUri by remember { mutableStateOf<Uri?>(null) }
                var statusText by remember { mutableStateOf("Belum ada video") }

                // ✅ Launcher Import Video (pakai SAF OpenDocument, bisa persist permission)
                val pickVideoLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        // ✅ Simpan permission agar bisa dibaca lagi saat export
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                            // beberapa device tidak perlu / sudah otomatis, aman diabaikan
                        }

                        inputVideoUri = uri
                        statusText = "Video diimport ✅\n$uri"
                    } else {
                        statusText = "Import dibatalkan"
                    }
                }

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

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                // SAF OpenDocument butuh array mimeTypes
                                pickVideoLauncher.launch(arrayOf("video/*"))
                            },
                            modifier = Modifier.fillMaxWidth(0.75f)
                        ) {
                            Text("Import Video")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = {
                                val uri = inputVideoUri
                                if (uri == null) {
                                    statusText = "Export gagal ❌\nKamu belum import video"
                                    return@Button
                                }

                                // ✅ Panggil Exporter di sini
                                // Wajib: pastikan fungsi Exporter.export(...) ada sesuai ini.
                                val result = Exporter.export(
                                    context = context,
                                    inputVideoUri = uri
                                )

                                statusText = if (result.ok) {
                                    "Export selesai ✅\n${result.message}"
                                } else {
                                    "Export gagal ❌\n${result.message}"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(0.5f)
                        ) {
                            Text("Export")
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Hasil export: Movies/AutoShorts/... (lihat File Manager)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
