package com.autoshorts.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.autoshorts.app.ai.SceneDetector
import com.autoshorts.app.ai.ViralScorer
import com.autoshorts.app.ai.ClipCandidate
import com.autoshorts.app.ai.HormoziCaption

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      MaterialTheme {
        AutoShortsScreen(  var candidates by remember { mutableStateOf<List<ClipCandidate>>(emptyList()) }
  var selectedIdx by remember { mutableStateOf(0) }
  var transcript by remember { mutableStateOf("") }
  
          onExport = { uri ->
            Exporter.exportToAppFolder(this@MainActivity, uri)
          }
        )
      }
    }
  }
}

@Composable
private fun AutoShortsScreen(
  onExport: (Uri) -> String
) {
  var videoUri by remember { mutableStateOf<Uri?>(null) }
  var status by remember { mutableStateOf("Klik Import Video") }

  val picker = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri ->
    videoUri = uri
    status = if (uri != null) "Video dipilih ✅" else "Batal memilih video"
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Text("AutoShorts MVP", style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(12.dp))

    Button(onClick = {
      picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
    }) { Text("Import Video") }

    Spacer(Modifier.height(12.dp))
    Text(status)     Spacer(Modifier.height(16.dp))

    Button(enabled = videoUri != null, onClick = {
      status = "Analyzing... (scene detect)"
      val cuts = SceneDetector.detectCuts(context = (LocalContext.current), uri = videoUri!!)
      val durMs = try {
        val mmr = android.media.MediaMetadataRetriever()
        mmr.setDataSource(LocalContext.current, videoUri!!)
        val d = (mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0").toLong()
        mmr.release()
        d
      } catch (_: Exception) { 0L }

      candidates = ViralScorer.scoreCandidates(durMs, cuts.map { it.timeMs })
      selectedIdx = 0
      status = if (candidates.isEmpty()) "Tidak ada kandidat clip" else "Top clip siap ✅"
    }) { Text("Analyze (AI)") }

    Spacer(Modifier.height(16.dp))
    if (candidates.isNotEmpty()) {
      Text("Top Kandidat:")
      Spacer(Modifier.height(8.dp))
      candidates.take(3).forEachIndexed { idx, c ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("#${idx+1}  ${c.startMs/1000}s - ${c.endMs/1000}s  (Skor ${c.score})")
          Button(onClick = { selectedIdx = idx }) { Text(if (selectedIdx==idx) "Dipilih" else "Pilih") }
        }
        Spacer(Modifier.height(6.dp))
        Text("Alasan: ${c.reason}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(10.dp))
      }

      OutlinedTextField(
        value = transcript,
        onValueChange = { transcript = it },
        label = { Text("Transcript (opsional, untuk caption Hormozi)") },
        modifier = Modifier.fillMaxWidth()
      )
    }
    

    Spacer(Modifier.height(24.dp))
    Button(
      enabled = videoUri != null,
      onClick = { status = onExport(videoUri!!) }
    ) { Text("Export") }
  }
}
