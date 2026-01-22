package com.autoshorts.app

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    data class ExportResult(
        val success: Boolean,
        val outputUri: Uri? = null,
        val userPath: String? = null,
        val errorMessage: String? = null
    )

    suspend fun exportToMovies(context: Context, inputVideoUri: Uri): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val fileName = "AutoShorts_$time.mp4"

                if (Build.VERSION.SDK_INT >= 29) {
                    // Android 10+ (Q+) -> MediaStore (tanpa WRITE_EXTERNAL_STORAGE)
                    exportWithMediaStoreQPlus(context, inputVideoUri, fileName)
                } else {
                    // Android 8/9 -> File API (butuh WRITE_EXTERNAL_STORAGE)
                    exportWithLegacyFileApi(context, inputVideoUri, fileName)
                }
            } catch (e: Exception) {
                ExportResult(
                    success = false,
                    errorMessage = e.message ?: e.toString()
                )
            }
        }
    }

    private fun exportWithMediaStoreQPlus(
        context: Context,
        inputUri: Uri,
        fileName: String
    ): ExportResult {
        val resolver = context.contentResolver

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/AutoShorts")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val outUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return ExportResult(false, errorMessage = "Gagal membuat file di MediaStore")

        try {
            resolver.openInputStream(inputUri).use { input ->
                if (input == null) return ExportResult(false, errorMessage = "Tidak bisa baca input video")
                resolver.openOutputStream(outUri).use { output ->
                    if (output == null) return ExportResult(false, errorMessage = "Tidak bisa tulis output video")
                    input.copyTo(output)
                }
            }

            // selesai tulis
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(outUri, values, null, null)

            return ExportResult(
                success = true,
                outputUri = outUri,
                userPath = "Movies/AutoShorts/$fileName"
            )
        } catch (e: Exception) {
            // cleanup kalau gagal
            resolver.delete(outUri, null, null)
            return ExportResult(false, errorMessage = e.message ?: e.toString())
        }
    }

    private fun exportWithLegacyFileApi(
        context: Context,
        inputUri: Uri,
        fileName: String
    ): ExportResult {
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val outDir = File(moviesDir, "AutoShorts")
        if (!outDir.exists()) outDir.mkdirs()

        val outFile = File(outDir, fileName)

        context.contentResolver.openInputStream(inputUri).use { input ->
            if (input == null) return ExportResult(false, errorMessage = "Tidak bisa baca input video")
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        }

        return ExportResult(
            success = true,
            outputUri = Uri.fromFile(outFile),
            userPath = "Movies/AutoShorts/$fileName"
        )
    }
}
