package com.autoshorts.app

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object Exporter {

  fun exportToDownloads(context: Context, inputUri: Uri): String {
    val resolver = context.contentResolver
    val fileName = "AutoShorts_${System.currentTimeMillis()}.mp4"

    return if (Build.VERSION.SDK_INT >= 29) {
      // Android 10+
      val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
        put(MediaStore.Downloads.MIME_TYPE, "video/mp4")
        put(MediaStore.Downloads.RELATIVE_PATH, "Download/AutoShorts")
      }

      val outUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: return "Gagal membuat file output"

      resolver.openInputStream(inputUri).use { input ->
        resolver.openOutputStream(outUri).use { output ->
          if (input == null || output == null) return "Gagal buka stream"
          input.copyTo(output)
        }
      }
      "Export selesai ✅ (Download/AutoShorts/$fileName)"
    } else {
      // Android 9 ke bawah
      val dir = File(context.getExternalFilesDir(null), "AutoShorts")
      if (!dir.exists()) dir.mkdirs()
      val outFile = File(dir, fileName)

      resolver.openInputStream(inputUri).use { input ->
        FileOutputStream(outFile).use { output ->
          if (input == null) return "Gagal buka video input"
          input.copyTo(output)
        }
      }
      "Export selesai ✅ (${outFile.absolutePath})"
    }
  }
}
