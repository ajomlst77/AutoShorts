package com.autoshorts.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    data class ExportResult(
        val folder: File,
        val videoFile: File,
        val metaFile: File,
        val srtFile: File
    )

    fun export(
        context: Context,
        videoUri: Uri,
        transcriptText: String,
        metaText: String,
        clipName: String = "clip"
    ): ExportResult {

        // 1) Root folder: /Android/data/<package>/files/AutoShorts
        val root = File(context.getExternalFilesDir(null), "AutoShorts")
        root.mkdirs()

        // 2) Sub folder per export: clip_yyyyMMdd_HHmmss
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val safeName = clipName.trim().ifEmpty { "clip" }
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")

        val outDir = File(root, "${safeName}_$stamp")
        outDir.mkdirs()

        // 3) Save meta.txt
        val metaFile = File(outDir, "meta.txt")
        metaFile.writeText(metaText)

        // 4) Save captions.srt
        val srtFile = File(outDir, "captions.srt")
        val srtContent = toSimpleSrt(transcriptText)
        srtFile.writeText(srtContent)

        // 5) Copy full video -> source.mp4
        val videoFile = File(outDir, "source.mp4")
        context.contentResolver.openInputStream(videoUri).use { input ->
            requireNotNull(input) { "Gagal membuka videoUri (inputStream null)" }
            FileOutputStream(videoFile).use { output ->
                input.copyTo(output)
            }
        }

        return ExportResult(
            folder = outDir,
            videoFile = videoFile,
            metaFile = metaFile,
            srtFile = srtFile
        )
    }

    private fun toSimpleSrt(text: String): String {
        val lines = text
            .replace("\r\n", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return "1\n00:00:00,000 --> 00:00:02,000\n(Empty)\n\n"
        }

        val sb = StringBuilder()
        var startMs = 0L
        val durMs = 2000L

        lines.forEachIndexed { idx, line ->
            val endMs = startMs + durMs
            sb.append("${idx + 1}\n")
            sb.append("${fmtSrtTime(startMs)} --> ${fmtSrtTime(endMs)}\n")
            sb.append(line)
            sb.append("\n\n")
            startMs = endMs
        }
        return sb.toString()
    }

    private fun fmtSrtTime(ms: Long): String {
        val h = ms / 3_600_000
        val m = (ms % 3_600_000) / 60_000
        val s = (ms % 60_000) / 1_000
        val msPart = ms % 1_000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", h, m, s, msPart)
    }
}
