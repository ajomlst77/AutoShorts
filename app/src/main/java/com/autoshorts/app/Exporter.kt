package com.autoshorts.app

object Exporter {

    fun export(meta: VideoMeta): String {
        // sementara cuma simulasi export
        return "Exported: ${meta.videoUri}"
    }
}
