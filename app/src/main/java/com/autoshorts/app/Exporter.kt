package com.autoshorts.app

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    data class ExportResult(
        val success: Boolean,
        val message: String,
        val outputFolder: String? = null
    )

    fun export(
        context: Context,
        inputVideoUri: Uri,
        transcriptText: String,
        metaText: String
    ): ExportResult {

        return try {

            // ===== Folder Movies/AutoShorts =====
            val baseDir = Environment.get        relativeDir: String,
        fileName: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, relativeDir)
            }

            val destUri = context.contentResolver.insert(
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                values
            ) ?: return

            val input = context.contentResolver.openInputStream(sourceUri) ?: return
            val output = context.contentResolver.openOutputStream(destUri) ?: return

            input.use { ins ->
                output.use { outs ->
                    ins.copyTo(outs, 1024 * 1024)
                }
            }

        } else {
            val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val dir = File(base, relativeDir.removePrefix("Movies/"))
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)

            val input = context.contentResolver.openInputStream(sourceUri) ?: return
            val output: OutputStream = FileOutputStream(file)

            input.use { ins ->
                output.use { outs ->
                    ins.copyTo(outs, 1024 * 1024)
                }
            }
        }
    }
}
