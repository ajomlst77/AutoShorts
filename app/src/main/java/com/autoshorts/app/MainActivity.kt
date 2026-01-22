package com.autoshorts.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                AppUI()
            }
        }
    }

    @Composable
    private fun AppUI() {
        val context = this@MainActivity
        val scope = rememberCoroutineScope()

        var inputVideoUri by remember { mutableStateOf<Uri?>(null) }
        var statusText by remember { mutableStateOf("") }
        var outputText by remember { mutableStateOf("") }

        // Picker: OpenDocument (lebih aman + bisa persist permission)
        val pickVideoLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                // Persist permission supaya tidak "permission denial" saat export
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // aman, kalau gagal tetap lanjut
                }

                inputVideoUri = uri
                statusText = "Video dipilih ✅"
                outputText = ""
            } else {
                statusText = "Batal pilih video."
            }
        }

        // Permission untuk Android 8/9 (API 26-28) kalau tulis ke folder Movies pakai File API
        val requestWritePermission = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                statusText = "Permission storage diberikan ✅, tekan Export lagi."
            } else {
                statusText = "Permission ditolak ❌ (Android 8/9 butuh izin untuk simpan file)"
            }
        }

        fun hasWritePermissionIfNeeded(ctx: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= 29) {
                true // Android 10+ pakai MediaStore, tidak butuh WRITE_EXTERNAL_STORAGE
            } else {
                ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

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

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    // filter video saja
                    pickVideoLauncher.launch(arrayOf("video/*"))
                }
            ) {
                Text("Import Video")
            }

            Spacer(Modifier.height(16.dp))

            if (statusText.isNotBlank()) {
                Text(statusText)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val uri = inputVideoUri
                    if (uri == null) {
                        statusText = "Pilih video dulu ❗"
                        return@Button
                    }

                    // Android 8/9 butuh WRITE permission kalau simpan pakai File API
                    if (!hasWritePermissionIfNeeded(context)) {
                        requestWritePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        return@Button
                    }

                    statusText = "Export berjalan... ⏳"
                    outputText = ""

                    scope.launch {
                        val result = Exporter.exportToMovies(context, uri)
                        if (result.success) {
                            statusText = "Export selesai ✅"
                            outputText = "Hasil export: ${result.userPath}"
                        } else {
                            statusText = "Export gagal ❌"
                            outputText = result.errorMessage ?: "Unknown error"
                        }
                    }
                }
            ) {
                Text("Export")
            }

            Spacer(Modifier.height(18.dp))

            if (outputText.isNotBlank()) {
                Text(outputText)
            }

            Spacer(Modifier.height(8.dp))
            Text("Catatan: cek di Gallery atau File Manager → Movies/AutoShorts")
        }
    }
}
