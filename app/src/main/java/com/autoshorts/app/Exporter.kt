package com.autoshorts.app

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    data class ExportResult(
        val folderName: String,
        val videoUri: Uri?,
        val metaUri: Uri?,
        val srtUri: Uri?
    )

    /**
     * STEP 3 Export:
     * - buat folder di Movies/AutoShorts/<timestamp>/
     * - simpan meta.txt
     * - simpan captions.srt
     * - copy video full (sementara)
     */
    fun export(
        context: Context,
        sourceVideoUri: Uri,
        transcriptText: String,
        metaText: String
    ): ExportResult {

        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val folderName = "AutoShorts/$ts" // di dalam Movies/

        val resolver = context.contentResolver

        // 1) Copy video full -> Movies/AutoShorts/<ts>/video.mp4
        val outVideoUri = createMediaStoreFile(
            resolver = resolver,
            relativePath = "Movies/$folderName",
            displayName = "video_full.mp4",
            mimeType = "video/mp4"
        )

        if (outVideoUri != null) {
            copyUri(resolver, sourceVideoUri, outVideoUri)
        }

        // 2) meta.txt
        val metaUri = createMediaStoreFile(
            resolver = resolver,
            relativePath = "Movies/$folderName",
            displayName = "meta.txt",
            mimeType = "text/plain"
        )
        if (metaUri != null) {
            writeText(resolver, metaUri, metaText)
        }

        // 3) captions.srt (sementara: 1 block panjang dari transcript)
        val srtText = buildSimpleSrtFromTranscript(transcriptText)
        val srtUri = createMediaStoreFile(
            resolver = resolver,
            relativePath = "Movies/$folderName",
            displayName = "captions.srt",
            mimeType = "application/x-subrip"
        )
        if (srtUri != null) {
            writeText(resolver, srtUri, srtText)
        }

        return ExportResult(
            folderName = "Movies/$folderName",
            videoUri = outVideoUri,
            metaUri = metaUri,
            srtUri = srtUri
        )
    }

    private fun buildSimpleSrtFromTranscript(transcript: String): String {
        val cleaned = transcript.trim().ifEmpty { "(kosong)" }
        // sementara: 1 subtitle 00:00:00 -> 00:00:20
        return """
1
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
