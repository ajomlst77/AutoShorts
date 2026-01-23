package com.autoshorts.app

object Exporter {

    fun export(meta: VideoMeta): String {
        return """
            {
              "videoUri": "${meta.videoUri}",
              "width": ${meta.width},
              "height": ${meta.height},
              "rotation": ${meta.rotation},
              "mimeType": "${meta.mimeType}",
              "fileSizeBytes": ${meta.fileSizeBytes}
            }
        """.trimIndent()
    }
}
