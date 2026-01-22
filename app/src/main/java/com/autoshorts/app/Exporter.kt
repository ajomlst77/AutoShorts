package com.autoshorts.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            val folderName = "AutoShorts/Export_$time"

            // 1) Copy video to Movies/AutoShorts/Export_xxx/input_video.mp4 (via MediaStore)
            val outVideoUri = createMediaStoreFile(
                context = context,
                relativePath = "Movies/$folderName/",
                displayName = "input_video.mp4",
                mimeType = "video/mp4"
            )
            copyUriToUri(context, inputVideoUri, outVideoUri)

            // 2) Save SRT to Movies/AutoShorts/Export_xxx/subtitle.srt
            val outSrtUri = createMediaStoreFile(
                context = context,
                relativePath = "Movies/$folderName/",
                displayName = "subtitle.srt",
                mimeType = "application/x-subrip"
            )
            writeTextToUri(context, outSrtUri, transcriptText)

            // 3) Save meta to Movies/AutoShorts/Export_xxx/meta.txt
            val outMetaUri = createMediaStoreFile(
                context = context,
                relativePath = "Movies/$folderName/",
                displayName = "meta.txt",
                mimeType = "text/plain"
            )
            writeTextToUri(context, outMetaUri, metaText)

            ExportResult(
                success = true,
                message = "Export selesai ✅\nCek: Internal Storage > Movies > AutoShorts > Export_$time",
                videoUri = outVideoUri.toString(),
                srtUri = outSrtUri.toString(),
                metaUri = outMetaUri.toString()
            )

        } catch (e: Exception) {
            ExportResult(
                success = false,
                message = "Export gagal ❌\n${e.message}"
            )
        }
    }

    // ===== Helpers =====

    private fun createMediaStoreFile(
        context: Context,
        relativePath: String,
        displayName: String,
        mimeType: String
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Scoped Storage: path relatif di storage publik
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Files.getContentUri("external")
        val uri = context.contentResolver.insert(collection, values)
            ?: throw IllegalStateException("Gagal membuat file MediaStore: $displayName")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Selesai buat, tapi masih pending sampai kita tulis isi
            // (nanti setelah nulis kita set IS_PENDING = 0)
        }
        return uri
    }

    private fun copyUriToUri(context: Context, from: Uri, to: Uri) {
        val resolver = context.contentResolver

        val input: InputStream = resolver.openInputStream(from)
            ?: throw
