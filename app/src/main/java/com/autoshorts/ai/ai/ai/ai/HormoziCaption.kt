package com.autoshorts.app.ai

object HormoziCaption {

  fun toHormoziLines(text: String): List<String> {
    val cleaned = text
      .replace(Regex("\\s+"), " ")
      .trim()

    if (cleaned.isEmpty()) return emptyList()

    val words = cleaned.split(" ")
    val out = mutableListOf<String>()

    var i = 0
    while (i < words.size) {
      val chunk = when {
        i + 4 <= words.size -> 4
        i + 3 <= words.size -> 3
        else -> 2.coerceAtMost(words.size - i)
      }
      out.add(words.subList(i, i + chunk).joinToString(" "))
      i += chunk
    }
    return out
  }

  fun toSrt(lines: List<String>, startMs: Long, endMs: Long): String {
    if (lines.isEmpty()) return ""

    val totalMs = (endMs - startMs).coerceAtLeast(1)
    val perLine = (totalMs / lines.size).coerceAtLeast(350)

    val sb = StringBuilder()
    var t = startMs
    for ((idx, line) in lines.withIndex()) {
      val s = t
      val e = minOf(endMs, t + perLine)

      sb.append(idx + 1).append("\n")
      sb.append(msToSrtTime(s)).append(" --> ").append(msToSrtTime(e)).append("\n")
      sb.append(line.uppercase()).append("\n\n")

      t = e
      if (t >= endMs) break
    }
    return sb.toString()
  }

  private fun msToSrtTime(ms: Long): String {
    val h = ms / 3600000
    val m = (ms % 3600000) / 60000
    val s = (ms % 60000) / 1000
    val ms2 = ms % 1000
    return "%02d:%02d:%02d,%03d".format(h, m, s, ms2)
  }
}
