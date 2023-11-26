package kz.just_code.workmanagerapp

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.UUID

class LoadImageWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    private val preferences = context.getSharedPreferences("Images list", Context.MODE_PRIVATE)

    override fun doWork(): Result {
        val url = inputData.getString(WorkerKey.IMAGE_URL.name)
        val bitmap = loadImage(url)
        return saveMediaToStorage(applicationContext, bitmap)?.let {
            val data = Data.Builder()
                .putString(WorkerKey.SAVED_IMAGE_PATH.name, it.toString())
                .build()

            Result.success(data)
        } ?: Result.failure()
    }

    private fun loadImage(url: String?): Bitmap? {
        return url?.parseUrl()?.let {
            val connection: HttpURLConnection?
            try {
                connection = it.openConnection() as HttpURLConnection
                connection.connect()
                BitmapFactory.decodeStream(BufferedInputStream(connection.inputStream))
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun saveMediaToStorage(context: Context, bitmap: Bitmap?): Uri? {
        val filename = "${UUID.randomUUID()}.jpg"
        var imageUri: Uri? = null
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }
                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }
        fos?.use {
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        saveImageName(imageUri)
        return imageUri
    }

    private fun saveImageName(image: Uri?) {
        image?.let {
            val images = Gson().fromJson(preferences.getString(WorkerKey.PHOTO_URL.name, "[]"), Array<String>::class.java).toMutableList()
            images.add(image.toString())

            val editor = preferences.edit()
            editor.putString(WorkerKey.PHOTO_URL.name, Gson().toJson(images))
            editor.apply()
        }
    }
}

fun String.parseUrl(): URL? {
    return try {
        URL(this)
    } catch (e: MalformedURLException) {
        null
    }
}

enum class WorkerKey {
    IMAGE_URL, SAVED_IMAGE_PATH, PHOTO_URL
}