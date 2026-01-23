package com.autoshorts.app

object Exporter {

    /**
     * Export TANPA context
     * Dipanggil dari MainActivity: Exporter.export(meta)
     */
    fun export(meta: VideoMeta): String {
        return """
            {
              "uri": "${meta.uri}",
              "width": ${meta.width},
              "height": ${meta.height},
              "rotation": ${meta.rotation},
              "mimeType": "${meta.mimeType}",
              "fileSizeBytes": ${meta.fileSizeBytes}
            }
        """.trimIndent()
    }
}
