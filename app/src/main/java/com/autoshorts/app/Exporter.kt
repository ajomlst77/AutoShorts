package com.autoshorts.app

import android.content.ContentResolver
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

        val folderName = defaultFolderName()
        val relativeDir = "Movies/AutoShorts/$folderName"

        return try {

            writeText(context, relativeDir, "meta.txt", metaText)
            writeText(context, relativeDir, "captions.srt", transcriptText)
            copyVideo(context, inputVideoUri, relativeDir, "video_full.mp4")

            ExportResult(
                true,
                "Export selesai ✅\nCek folder: Movies/AutoShorts/$folderName"
            )

        } catch (e: Exception) {
            ExportResult(false, "Export gagal: ${e.message}")
        }
    }

    private fun defaultFolderName(): String {
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
    private fun defaultFolderName(): String {
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "export_${sdf.format(Date())}"
    }

    // ---------- TEXT WRITER ----------
    private fun writeTextToMovies(
        context: Context,
        relativeDir: String,
        fileName: String,
        mime: String,
        content: String
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeTextMediaStore(context.contentResolver, relativeDir, fileName, mime, content)
        } else {
            writeTextLegacy(relativeDir, fileName, content)
        }
    }

    private fun writeTextMediaStore(
        resolver: ContentResolver,
        relativeDir: String,
        fileName: String,
        mime: String,
        content: String
    ): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
        }

        val collection = MediaStore.Files.getContentUri("external")
        val fileUri = resolver.insert(collection, values) ?: return false

        return resolver.openOutputStream(fileUri, "w")?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
            out.flush()
            true
        } ?: false
    }

    private fun writeTextLegacy(relativeDir: String, fileName: String, content: String): Boolean {
        // relativeDir contoh: Movies/AutoShorts/export_xxx
        val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val targetDir = File(base, relativeDir.removePrefix("Movies/"))
        if (!targetDir.exists()) targetDir.mkdirs()
        val outFile = File(targetDir, fileName)
        return try {
            FileOutputStream(outFile).use { out ->
                out.write(content.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // ---------- VIDEO COPIER ----------
    private fun copyVideoToMovies(
        context: Context,
        sourceUri: Uri,
        relativeDir: String,
        fileName: String
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            copyVideoMediaStore(context.contentResolver, sourceUri, relativeDir, fileName)
        } else {
            copyVideoLegacy(context.contentResolver, sourceUri, relativeDir, fileName)
        }
    }

    private fun copyVideoMediaStore(
        resolver: ContentResolver,
        sourceUri: Uri,
        relativeDir: String,
        fileName: String
    ): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, relativeDir)
        }

        val videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val destUri = resolver.insert(videoCollection, values) ?: return false

        val input = resolver.openInputStream(sourceUri) ?: return false
        val output = resolver.openOutputStream(destUri, "w") ?: return false

        return input.use { ins ->
            output.use { outs ->
                ins.copyTo(outs, bufferSize = 1024 * 1024) // 1MB buffer
                outs.flush()
                true
            }
        }
    }

    private fun copyVideoLegacy(
        resolver: ContentResolver,
        sourceUri: Uri,
        relativeDir: String,
        fileName: String
    ): Boolean {
        val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val targetDir = File(base, relativeDir.removePrefix("Movies/"))
        if (!targetDir.exists()) targetDir.mkdirs()

        val destFile = File(targetDir, fileName)

        val input = resolver.openInputStream(sourceUri) ?: return false
        val output: OutputStream = FileOutputStream(destFile)

        return input.use { ins ->
            output.use { outs ->
                ins.copyTo(outs, bufferSize = 1024 * 1024)
                outs.flush()
                true
            }
        }
    }
}1
00:00:00,000 --> 00:00:20,000
$cleaned

""".trimIndent()
    }

    private fun createMediaStoreFile(
        resolver: ContentResolver,
        relativePath: String,
        displayName: String,
        mimeType: String
    ): Uri? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                }
            }

            val collection = MediaStore.Files.getContentUri("external")
            resolver.insert(collection, values)
        } catch (e: Exception) {
            null
        }
    }

    private fun copyUri(resolver: ContentResolver, from: Uri, to: Uri) {
        resolver.openInputStream(from)?.use { input ->
            resolver.openOutputStream(to)?.use { output ->
                input.copyTo(output)
                output.flush()
            }
        }
    }

    private fun writeText(resolver: ContentResolver, to: Uri, text: String) {
        resolver.openOutputStream(to)?.use { out ->
            BufferedWriter(OutputStreamWriter(out)).use { w ->
                w.write(text)
                w.flush()
            }
        }
    }
}
