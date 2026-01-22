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
                AppUI()
            }
        }
    }

    @Composable
    fun AppUI() {
        var status by remember { mutableStateOf("Siap") }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text("AutoShorts")

            Spacer(modifier = Modifier.height(20.dp))

            Button(onClick = {
                val path = Exporter.export(applicationContext)
                status = "Export sukses:\n$path"

                Toast.makeText(
                    applicationContext,
                    "Export OK",
                    Toast.LENGTH_SHORT
                ).show()
            }) {
                Text("Export")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(status)
        }
    }
}
