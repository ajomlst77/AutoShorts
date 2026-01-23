package com.autoshorts.app

data class VideoMeta(
    val uri: String,
    val width: Int = 0,
    val height: Int = 0,
    val rotation: Int = 0,
    val mimeType: String = "",
    val fileSizeBytes: Long = 0L
)
