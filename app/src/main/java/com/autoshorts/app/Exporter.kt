package com.autoshorts.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object Exporter {

    data class ExportResult(
        val success: Boolean,
        val message: String,
        val videoUri: String? = null,
        val srtUri: String? = null,
        val metaUri: String? = null
    )

    fun export(
        context: Context,
        inputVideoUri: Uri,
        transcriptText: String,
        metaText: String
    ): ExportResult {

        return try {
            val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val folder = "AutoShorts/Export_$time/"

            val videoOut = createFile(context, folder, "input_video.mp4", "video/mp4")
            copyUri(context, inputVideoUri, videoOut)

            val srtOut = createFile(context, folder, "subtitle.srt", "application/x-subrip")
            writeText(context, srtOut, transcriptText)

            val metaOut = createFile(context, folder, "meta.txt", "text/plain")
            writeText(context, metaOut, metaText)

            ExportResult(
                true,
                "Export sukses ✅\nCek: Movies/AutoShorts/Export_$time",
                videoOut.toString(),
                srtOut.toString(),
                metaOut.toString()
            )

        } catch (e: Exception) {
            ExportResult(false, "Export gagal ❌\n${e.message}")
        }
    }

    private fun createFile(
        context: Context,
        relativePath: String,
        name: String,
        mime: String
    ): Uri {

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/$relativePath")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(
            MediaStore.Files.getContentUri("external"),
            values
        ) ?: throw Exception("Gagal buat file: $name")

        return uri
    }

    private fun copyUri(context: Context, from: Uri, to: Uri) {
        val resolver = context.contentResolver

        val input: InputStream = resolver.openInputStream(from)
            ?: throw Exception("Tidak bisa baca video input")

        resolver.openOutputStream(to)?.use { out ->
            input.use { it.copyTo(out) }
        }

        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(to, values, null, null)
        }
    }

    private fun writeText(context: Context, uri: Uri, text: String) {
        val resolver = context.contentResolver

        resolver.openOutputStream(uri)?.use { out ->
            out.write(text.toByteArray(Charsets.UTF_8))
            out.flush()
        }

        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
    }
}
