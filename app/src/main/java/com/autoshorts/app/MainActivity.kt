package com.autoshorts.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AutoShortsScreen()
                }
            }
        }
    }
}

@Composable
private fun AutoShortsScreen() {
    val context = LocalContext.current

    var inputVideoUri by remember { mutableStateOf<Uri?>(null) }
    var statusText by remember { mutableStateOf("") }

    // ✅ Import video launcher (SAF)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            statusText = "Import dibatalkan"
            return@rememberLauncherForActivityResult
        }

        // ✅ Simpan izin baca jangka panjang (persistable) supaya bisa dibaca saat export
        try {
            val flags = (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (_: Exception) {
            // Beberapa file manager tidak support persistable; tetap lanjut
        }

        inputVideoUri = uri
        statusText = "Video terpilih ✅\n$uri"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "AutoShorts MVP",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp, bottom = 30.dp)
        )

        Button(
            onClick = {
                // ✅ filter mp4/mov dll
                importLauncher.launch(arrayOf("video/*"))
            },
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(54.dp)
        ) {
            Text("Import Video", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (statusText.isNotBlank()) {
            Text(
                text = statusText,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        Button(
            onClick = {
                val uri = inputVideoUri
                if (uri == null) {
                    statusText = "Export gagal ❌\nKamu belum pilih video"
                    return@Button
                }

                try {
                    // ✅ PAKAI parameter yang benar: inputVideoUri (bukan sourceVideoUri)
                    val result = Exporter.export(
                        context = context,
                        inputVideoUri = uri
                    )

                    statusText = if (result.success) {
                        "Export selesai ✅\n${result.outputPath}"
                    } else {
                        "Export gagal ❌\n${result.errorMessage}"
                    }
                } catch (e: Exception) {
                    statusText = "Export gagal ❌\n${e.message}"
                }
            },
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(52.dp)
        ) {
            Text("Export", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Hasil export: Movies/AutoShorts/... (lihat File Manager)",
            fontSize = 14.sp
        )
    }
}
