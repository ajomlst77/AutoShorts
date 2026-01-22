package com.autoshorts.app

import android.content.Context
import android.os.Environment
import java.io.File

object Exporter {

    fun export(context: Context): String {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "AutoShorts"
        )

        if (!dir.exists()) {
            dir.mkdirs()
        }

        val outputFile = File(dir, "result_${System.currentTimeMillis()}.txt")
        outputFile.writeText("Export berhasil")

        return outputFile.absolutePath
    }
}
