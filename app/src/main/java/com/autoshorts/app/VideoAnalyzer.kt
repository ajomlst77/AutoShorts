package com.autoshorts.app

import android.net.Uri

data class VideoMeta(
    val videoUri: Uri,
    val width: Int = 0,
    val height: Int = 0,
    val rotation: Int = 0,
    val mimeType: String = "",
    val fileSizeBytes: Long = 0L
)
