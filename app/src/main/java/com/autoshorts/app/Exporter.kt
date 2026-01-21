package com.autoshorts.app

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object Exporter {

    fun export(
        context: Context,
        videoUri: Uri,
        transcript: String,
        meta: String
    ) {

        val folder = File(context.getExternalFilesDir(null), "AutoShorts")
        folder.mkdirs()

        File(folder, "meta.txt").writeText(meta)
        File(folder, "transcript.srt").writeText(transcript)

        val input = context.contentResolver.openInputStream(videoUri) ?: return
        val output = FileOutputStream(File(folder, "source.mp4"))

        input.copyTo(output)
        input.close()
        output.close()
    }
}
