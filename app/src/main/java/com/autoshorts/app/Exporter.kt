package com.autoshorts.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    data class ExportResult(
        val ok: Boolean,
        val message: String
    )

    fun export(
        context: Context,
        inputVideoUri: Uri,
        transcriptText: String,
        metaText: String
    ): ExportResult {

        val folderName = generateFolderName()
        val relativeDir = "Movies/AutoShorts/$folderName"

        return try {

            writeText(context, relativeDir, "meta.txt", metaText)
            writeText(context, relativeDir, "captions.srt", transcriptText)
            copyVideo(context, inputVideoUri, relativeDir, "video_full.mp4")

            ExportResult(true, "Export selesai ✅\nCek Movies/AutoShorts/$folderName")

        } catch (e: Exception) {
            ExportResult(false, "Export gagal: ${e.message}")
        }
    }

    private fun generateFolderName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "export_${sdf.format(Date())}"
    }

    private fun writeText(
        context: Context,
        relativeDir: String,
        fileName: String,
        content: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Files.getContentUri("external"),
                values
            ) ?: return

            context.contentResolver.openOutputStream(uri)?.use {
                it.write(content.toByteArray())
                it.flush()
            }

        } else {
            val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            val dir = File(base, relativeDir.removePrefix("Movies/"))
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use {
                it.write(content.toByteArray())
                it.flush()
            }
        }
    }

    private fun copyVideo(
        context: Context,
        sourceUri: Uri,
        relativeDir: String,
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
