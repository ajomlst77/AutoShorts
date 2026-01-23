package com.autoshorts.app

import android.content.ContentResolver
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

data class VideoMeta(
    val uri: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val mimeType: String?,
    val fileSizeBytes: Long?
) {
    val aspectRatio: String
        get() = if (width > 0 && height > 0) "${width}:${height}" else "unknown"

    val durationSec: Long
        get() = durationMs / 1000L
}

object VideoAnalyzer {

    fun analyze(context: Context, uri: Uri): VideoMeta {
        val resolver: ContentResolver = context.contentResolver
        val retriever = MediaMetadataRetriever()

        try {
            retriever.setDataSource(context, uri)

            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            val width = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0

            val height = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0

            val rotation = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0

            val mimeType = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)

            val fileSizeBytes = tryGetFileSize(resolver, uri)

            return VideoMeta(
                uri = uri.toString(),
                durationMs = durationMs,
                width = width,
                height = height,
                rotation = rotation,
                mimeType = mimeType,
                fileSizeBytes = fileSizeBytes
            )
        } finally {
            try { retriever.release() } catch (_: Throwable) {}
        }
    }

    private fun tryGetFileSize(resolver: ContentResolver, uri: Uri): Long? {
        // Cara aman di Android lama: openAssetFileDescriptor
        return try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                val len = afd.length
                if (len >= 0) len else null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
