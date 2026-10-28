package com.adspro.app.utils

import android.content.Context
import android.net.Uri
import java.io.*

object FileUtils {
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val file = File.createTempFile("upload_", ".tmp", context.cacheDir)
            val output = FileOutputStream(file)
            input.copyTo(output)
            input.close()
            output.close()
            file
        } catch (e: Exception) { null }
    }
}
