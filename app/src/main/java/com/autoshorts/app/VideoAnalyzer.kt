package com.autoshorts.app

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri

object VideoAnalyzer {

    fun analyze(context: Context, videoUri: Uri): VideoMeta {
        val retriever = MediaMetadataRetriever()
        var width = 0
        var height = 0
        var rotation = 0
        var mime = ""
        var sizeBytes = 0L

        try {
            retriever.setDataSource(context, videoUri)

            width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0

            height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                ?.toIntOrNull() ?: 0

            rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                ?.toIntOrNull() ?: 0

            mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: ""
        } catch (_: Exception) {
            // ignore
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        // file size dari ContentResolver
        try {
            context.contentResolver.openAssetFileDescriptor(videoUri, "r")?.use { afd ->
                sizeBytes = afd.length
            }
        } catch (_: Exception) {
            // ignore
        }

        return VideoMeta(
            videoUri = videoUri.toString(),
            width = width,
            height = height,
            rotation = rotation,
            mimeType = mime,
            fileSizeBytes = sizeBytes
        )
    }
}
