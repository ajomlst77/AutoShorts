package com.autoshorts.app

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object Exporter {

    fun export(
        context: Context,
        videoUri: Uri,
        transcript: String,
        meta: String
    ) {

        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val root = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "AutoShorts/$time"
        )

        if (!root.exists()) root.mkdirs()

        // --- COPY VIDEO ---
        val videoOut = File(root, "source.mp4")
        context.contentResolver.openInputStream(videoUri)?.use { input ->
            videoOut.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // --- SAVE META ---
        File(root, "meta.txt").writeText(meta)

        // --- SAVE SRT ---
        File(root, "subtitle.srt").writeText(buildSrt(transcript))
    }

    private fun buildSrt(text: String): String {
        return """
1
00:00:00,000 --> 00:00:03,000
$text
""".trimIndent()
    }
}
