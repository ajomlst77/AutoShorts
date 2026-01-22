package com.autoshorts.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
                AutoShortsUI()
            }
        }
    }

    @Composable
    fun AutoShortsUI() {
        var status by remember { mutableStateOf("Siap export") }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("AutoShorts MVP")

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    // 🔥 INI FIX UTAMA
                    val resultPath = Exporter.export(applicationContext)
                    status = "Hasil export:\n$resultPath"

                    Toast.makeText(
                        applicationContext,
                        "Export sukses",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            ) {
                Text("Export")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(status)
        }
    }
}
