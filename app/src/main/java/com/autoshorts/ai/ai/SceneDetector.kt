package com.autoshorts.app.ai

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import kotlin.math.abs

data class SceneCut(val timeMs: Long)

object SceneDetector {

  /**
   * Deteksi pergantian scene ringan:
   * - ambil frame tiap stepMs
   * - bandingkan "kecerahan rata-rata" (luma kasar)
   */
  fun detectCuts(context: Context, uri: Uri, stepMs: Long = 700, threshold: Int = 18): List<SceneCut> {
    val mmr = MediaMetadataRetriever()
    val cuts = mutableListOf<SceneCut>()

    try {
      mmr.setDataSource(context, uri)

      val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "0"
      val durationMs = durStr.toLongOrNull() ?: 0L
      if (durationMs <= 0) return emptyList()

      var prevLuma: Int? = null
      var t = 0L
      while (t < durationMs) {
        val bmp = mmr.getFrameAtTime(t * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        val luma = bmp?.let { quickLuma(it) } ?: 0
        bmp?.recycle()

        val p = prevLuma
        if (p != null) {
          val diff = abs(luma - p)
          if (diff >= threshold) cuts.add(SceneCut(t))
        }
        prevLuma = luma
        t += stepMs
      }
      return cuts
    } catch (_: Exception) {
      return emptyList()
    } finally {
      try { mmr.release() } catch (_: Exception) {}
    }
  }

  // Luma kasar: sampling beberapa pixel biar cepat
  private fun quickLuma(bmp: android.graphics.Bitmap): Int {
    val w = bmp.width
    val h = bmp.height
    val sx = maxOf(1, w / 24)
    val sy = maxOf(1, h / 24)

    var sum = 0L
    var count = 0
    var y = 0
    while (y < h) {
      var x = 0
      while (x < w) {
        val c = bmp.getPixel(x, y)
        val r = (c shr 16) and 0xFF
        val g = (c shr 8) and 0xFF
        val b = (c) and 0xFF
        // approximate luma
        val l = (r * 30 + g * 59 + b * 11) / 100
        sum += l
        count++
        x += sx
      }
      y += sy
    }
    return if (count == 0) 0 else (sum / count).toInt()
  }
}
  
