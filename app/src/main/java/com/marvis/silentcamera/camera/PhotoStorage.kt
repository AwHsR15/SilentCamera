package com.marvis.silentcamera.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PhotoStorage {
    fun values():ContentValues {
        val name="SilentCamera-${SimpleDateFormat("yyyyMMdd-HHmmss-SSS",Locale.ROOT).format(Date())}.jpg"
        return ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,name);put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
            if(Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/SilentCamera")
            else {
                @Suppress("DEPRECATION")
                val dir=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"SilentCamera")
                check(dir.exists() || dir.mkdirs()){"Cannot create photo directory"}
                @Suppress("DEPRECATION")
                put(MediaStore.Images.Media.DATA,File(dir,name).absolutePath)
            }
        }
    }
    fun save(context:Context,bitmap:Bitmap):Uri {
        val values=values().apply{if(Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.IS_PENDING,1)}
        val resolver=context.contentResolver
        val uri=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("Cannot create gallery entry")
        try {
            val output=resolver.openOutputStream(uri) ?: error("Cannot open photo output")
            output.use{check(bitmap.compress(Bitmap.CompressFormat.JPEG,94,it)){"JPEG encoding failed"}}
            if(Build.VERSION.SDK_INT>=29)resolver.update(uri,ContentValues().apply{put(MediaStore.Images.Media.IS_PENDING,0)},null,null)
            return uri
        }catch(e:Exception){resolver.delete(uri,null,null);throw e}
    }
}
