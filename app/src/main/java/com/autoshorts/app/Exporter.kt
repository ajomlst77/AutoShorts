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
        val srtFile: File,
        val metaFile: File
    )

    /**
     * STEP 3:
     * - bikin folder output
     * - simpan meta.txt + captions.srt
     * - copy video full jadi source.mp4
     */
    fun export(
        context: Context,
        videoUri: Uri,
        transcriptText: String,
        metaText: String,
        clipName: String
    ): ExportResult {

        // 1) Tentukan base folder (aman tanpa permission tambahan)
        val baseDir = File(context.getExternalFilesDir(null), "AutoShorts")
        if (!baseDir.exists()) baseDir.mkdirs()

        // 2) Buat folder unik per export
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val outDir = File(baseDir, "${clipName}_$stamp")
        if (!outDir.exists()) outDir.mkdirs()

        // 3) File output
        val videoOut = File(outDir, "source.mp4")
        val metaOut = File(outDir, "meta.txt")
        val srtOut = File(outDir, "captions.srt")

        // 4) Copy video full (sementara)
        copyUriToFile(context, videoUri, videoOut)

        // 5) Simpan meta.txt
        metaOut.writeText(metaText)

        // 6) Buat SRT sederhana dari transcriptText
        val srtText = transcriptToSimpleSrt(transcriptText)
        srtOut.writeText(srtText)

        return ExportResult(
            folder = outDir,
            videoFile = videoOut,
            srtFile = srtOut,
            metaFile = metaOut
        )
    }

    private fun copyUriToFile(context: Context, uri: Uri, outFile: File) {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Gagal membuka video dari Uri" }
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * SRT super sederhana:
     * - setiap baris transcript = 2 detik
     * - kalau kosong, bikin 1 caption default
     */
    private fun transcriptToSimpleSrt(transcript: String): String {
        val lines = transcript
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val safeLines = if (lines.isEmpty()) listOf("Caption belum ada") else lines

        val sb = StringBuilder()
        var startMs = 0L
        val durMs = 2000L

        safeLines.forEachIndexed { idx, text ->
            val endMs = startMs + durMs
            sb.append(idx + 1).append("\n")
            sb.append(msToSrtTime(startMs))
                .append(" --> ")
                .append(msToSrtTime(endMs))
                .append("\n")
            sb.append(text).append("\n\n")
            startMs = endMs
        }

        return sb.toString()
    }

    private fun msToSrtTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val milli = ms % 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = (totalSeconds / 3600)

        fun pad2(v: Long) = v.toString().padStart(2, '0')
        fun pad3(v: Long) = v.toString().padStart(3, '0')

        return "${pad2(hours)}:${pad2(minutes)}:${pad2(seconds)},${pad3(milli)}"
    }
}
