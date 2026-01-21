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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppUI()
        }
    }
}

@Composable
fun AppUI() {
    val context = LocalContext.current
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("") }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        videoUri = uri
    }

    Column(modifier = Modifier.padding(16.dp)) {

        Button(onClick = {
            pickLauncher.launch("video/*")
        }) {
            Text("Import Video")
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (videoUri != null) {
            Text("Video dipilih ✔")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            enabled = videoUri != null,
            onClick = {
                val baseName = "clip_${System.currentTimeMillis()}"

                val r1 = Exporter.exportToAppFolder(context, videoUri!!)
                val r2
