package com.autoshorts.app.ai

data class ClipCandidate(
  val startMs: Long,
  val endMs: Long,
  val score: Int,
  val reason: String
)

object ViralScorer {

  /**
   * Skor "AI-lite" (0-100):
   * - makin banyak cut dalam durasi → cenderung dinamis
   * - durasi ideal: 35-60 detik
   * - penalti kalau terlalu panjang
   */
  fun scoreCandidates(
    durationMs: Long,
    cutsMs: List<Long>,
    targetDurMs: Long = 45_000
  ): List<ClipCandidate> {
    if (durationMs <= 5_000) return emptyList()

    val windows = listOf(35_000L, 45_000L, 60_000L)
    val candidates = mutableListOf<ClipCandidate>()

    for (w in windows) {
      var start = 0L
      while (start + w <= durationMs) {
        val end = start + w
        val cutCount = cutsMs.count { it in start..end }
        val pacing = (cutCount * 8).coerceAtMost(35) // max 35 poin
        val lengthFit = (35 - (kotlin.math.abs(w - targetDurMs) / 1000).toInt()).coerceIn(5, 35)
        val base = 25

        val score = (base + pacing + lengthFit).coerceIn(0, 100)
        val reason = "cut=$cutCount, dur=${w/1000}s"

        candidates.add(ClipCandidate(start, end, score, reason))
        start += 15_000L // geser 15 detik biar tidak terlalu banyak
      }
    }

    return candidates.sortedByDescending { it.score }.take(12)
  }
}
