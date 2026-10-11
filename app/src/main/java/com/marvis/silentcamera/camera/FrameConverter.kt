package com.marvis.silentcamera.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.marvis.silentcamera.core.FramePixels

data class PreviewFrame(val width:Int,val height:Int,val rotation:Int,val pixels:IntArray) {
    fun bitmap():Bitmap {
        val source=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888)
        if(rotation==0)return source
        val rotated=Bitmap.createBitmap(source,0,0,width,height,Matrix().apply{postRotate(rotation.toFloat())},true)
        if(rotated!==source)source.recycle()
        return rotated
    }
}
object FrameConverter {
    fun convert(image:ImageProxy):PreviewFrame {
        require(image.planes.size==3)
        val arrays=image.planes.map{plane->plane.buffer.duplicate().let{buffer->ByteArray(buffer.remaining()).also{buffer.get(it)}}}
        val p=image.planes
        return PreviewFrame(image.width,image.height,image.imageInfo.rotationDegrees,
            FramePixels.toArgb(image.width,image.height,arrays[0],p[0].rowStride,p[0].pixelStride,
                arrays[1],p[1].rowStride,p[1].pixelStride,arrays[2],p[2].rowStride,p[2].pixelStride))
    }
}
