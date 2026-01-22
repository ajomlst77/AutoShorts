package com.autoshorts.app

import android.os.Environment
import java.io.File

object Exporter {

    fun export(): String {
        // Folder: Movies/AutoShorts
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "AutoShorts"
        )

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        // File hasil export
        val outputFile = File(
            outputDir,
            "export_${System.currentTimeMillis()}.txt"
        )

        // Isi contoh (nanti bisa diganti hasil video / proses lain)
        outputFile.writeText("Export berhasil dari AutoShorts")

        return outputFile.absolutePath
    }
}
