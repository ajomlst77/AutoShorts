package com.autoshorts.app

object Exporter {

    /**
     * Export sederhana (sementara).
     * Nanti kalau kamu mau, bisa kita upgrade export beneran (copy video / render / dll).
     */
    fun export(meta: VideoMeta): String {
        // Untuk sekarang: balikin string hasil export (dummy) biar app jalan dulu
        return "Export OK: ${meta.videoUri}"
    }
}
