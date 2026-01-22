package com.autoshorts.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var selectedVideoUri: Uri? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < 29) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        setContent {
            AppUI()
        }
    }

    @Composable
    fun AppUI() {
        var status by remember { mutableStateOf("") }

        val videoPicker =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                selectedVideoUri = uri
                status = if (uri != null) "Video dipilih ✅" else "Batal pilih video"
            }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Text("AutoShorts MVP", style = MaterialTheme.typography.headlineMedium)

            Spacer(Modifier.height(24.dp))

            Button(onClick = { videoPicker.launch("video/*") }) {
                Text("Import Video")
            }

            Spacer(Modifier.height(16.dp))

            Text(status)

            Spacer(Modifier.height(24.dp))

            Button(onClick = {
                val uri = selectedVideoUri
                if (uri == null) {
                    status = "Pilih video dulu ❌"
                    return@Button
                }

                val result = Exporter.export(
                    context = this@MainActivity,
                    inputVideoUri = uri,
                    transcriptText = "Subtitle contoh",
                    metaText = "Meta data contoh"
                )

                status = result.message

            }) {
                Text("Export")
            }

            Spacer(Modifier.height(16.dp))

            Text("Hasil export: Movies/AutoShorts/...")
        }
    }
}
