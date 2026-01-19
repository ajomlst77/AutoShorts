package com.autoshorts.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object Exporter {
  fun exportToAppFolder(context: Context, inputUri: Uri): String {
    return try {
      val dir = File(context.getExternalFilesDir(null), "AutoShorts")
      if (!dir.exists()) dir.mkdirs()

      val outFile = File(dir, "AutoShorts_${System.currentTimeMillis()}.mp4")

      context.contentResolver.openInputStream(inputUri).use { input ->
        if (input == null) return "Gagal membuka video input"
        FileOutputStream(outFile).use { output ->
          input.copyTo(output)
        }
      }

      "Export selesai ✅\n${outFile.absolutePath}"
    } catch (e: Exception) {
      "Export gagal ❌\n${e.message}"
    }
  }
}