package com.autoshorts.app

data class VideoMeta(
    val videoUri: String,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val mimeType: String,
    val fileSizeBytes: Long
)
