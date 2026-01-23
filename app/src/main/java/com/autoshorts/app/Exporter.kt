package com.autoshorts.app

object Exporter {

    /**
     * TANPA parameter context.
     * Return: String (hasil export, misalnya JSON text)
     */
    fun export(meta: VideoMeta): String {
        // Export sederhana: bikin JSON string dari meta
        // (Kalau kamu mau format lain, bilang ya)
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
