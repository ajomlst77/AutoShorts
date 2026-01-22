package com.autoshorts.app

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Exporter {

    data class ExportResult(
        val success: Boolean,
        val message: String,
        val outputFolder: String? = null
    )

    fun export(
        context: Context,
        inputVideoUri: Uri,
        transcriptText: String,
        metaText: String
    ): ExportResult {

        return try {

            val moviesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES
            )

            val rootFolder = File(moviesDir, "AutoShorts")
            if (!rootFolder.exists()) rootFolder.mkdirs()

            val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val exportFolder = File(rootFolder, "Export_$time")
            exportFolder.mkdirs()

            // Copy video
            val videoFile = File(exportFolder, "input_video.mp4")
            copyUriToFile(context, inputVideoUri, videoFile)

            // Save transcript
            val srtFile = File(exportFolder, "subtitle.srt")
            srtFile.writeText(transcriptText)

            // Save meta
            val metaFile = File(exportFolder, "meta.txt")
            metaFile.writeText(metaText)

            ExportResult(
                success = true,
                message = "Export selesai ✅\n${exportFolder.absolutePath}",
                outputFolder = exportFolder.absolutePath
            )

        } catch (e: Exception) {
            ExportResult(
                success = false,
                message = "Export gagal ❌\n${e.message}"
            )
        }
    }

    private fun copyUriToFile(
        context: Context,
        uri: Uri,
        outFile: File
    ) {
        val inputStream: InputStream? =
            context.contentResolver.openInputStream(uri)

        val outputStream = FileOutputStream(outFile)

        inputStream?.copyTo(outputStream)

        inputStream?.close()
        outputStream.close()
    }
}
