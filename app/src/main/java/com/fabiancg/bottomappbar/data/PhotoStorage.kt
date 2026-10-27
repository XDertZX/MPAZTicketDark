package com.fabiancg.bottomappbar.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException


object PhotoStorage {

    private fun dir(context: Context): File = File(context.filesDir, "photos").also { it.mkdirs() }

    fun fileFor(context: Context, name: String): File = File(dir(context), File(name).name)

    private fun newName() = "IMG_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"

    fun newCameraTarget(context: Context): Pair<String, Uri> {
        val name = newName()
        val file = fileFor(context, name).also { it.createNewFile() }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return name to uri
    }

    fun copyFromUri(context: Context, uri: Uri): String? {
        val name = newName()
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            input.use { source -> fileFor(context, name).outputStream().use { out -> source.copyTo(out) } }
            name
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    fun delete(context: Context, name: String) {
        fileFor(context, name).delete()
    }

    fun loadBitmap(context: Context, name: String, maxSide: Int = 1024): Bitmap? {
        val file = fileFor(context, name)
        if (!file.exists() || file.length() == 0L) return null
        val path = file.absolutePath

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxSide) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeFile(path, options) ?: return null

        val degrees = try {
            when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } catch (e: IOException) {
            0f
        }
        if (degrees == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
