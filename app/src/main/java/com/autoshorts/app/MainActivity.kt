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

    // simpan video terpilih
    private var selectedVideoUri: Uri? = null

    // launcher untuk pilih video
    private val pickVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedVideoUri = uri

                // ambil persist permission (biar aman kalau file manager tertentu)
                try {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                    // beberapa provider tidak support, aman diabaikan
                }

                Toast.makeText(this, "Video dipilih ✅", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Batal pilih video", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super
