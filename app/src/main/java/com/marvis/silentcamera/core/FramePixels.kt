package com.marvis.silentcamera.core

object FramePixels {
    fun toArgb(width:Int,height:Int,y:ByteArray,yRow:Int,yPixel:Int,u:ByteArray,uRow:Int,uPixel:Int,v:ByteArray,vRow:Int,vPixel:Int):IntArray {
        require(width>0 && height>0 && width.toLong()*height<=8_294_400)
        fun valid(plane:ByteArray,w:Int,h:Int,row:Int,pixel:Int):Boolean = row>0 && pixel>0 &&
            (h-1).toLong()*row+(w-1).toLong()*pixel<plane.size
        require(valid(y,width,height,yRow,yPixel) && valid(u,(width+1)/2,(height+1)/2,uRow,uPixel) && valid(v,(width+1)/2,(height+1)/2,vRow,vPixel))
        val out=IntArray(width*height)
        for(row in 0 until height)for(col in 0 until width) {
            val yy=((y[row*yRow+col*yPixel].toInt() and 255)-16).coerceAtLeast(0)
            val uu=(u[(row/2)*uRow+(col/2)*uPixel].toInt() and 255)-128
            val vv=(v[(row/2)*vRow+(col/2)*vPixel].toInt() and 255)-128
            val red=((298*yy+409*vv+128) shr 8).coerceIn(0,255)
            val green=((298*yy-100*uu-208*vv+128) shr 8).coerceIn(0,255)
            val blue=((298*yy+516*uu+128) shr 8).coerceIn(0,255)
            out[row*width+col]=0xff000000.toInt() or (red shl 16) or (green shl 8) or blue
        }
        return out
    }
    fun rotatedSize(width:Int,height:Int,rotation:Int):Pair<Int,Int> {
        require(width>0 && height>0 && rotation in setOf(0,90,180,270))
        return if(rotation==90 || rotation==270)height to width else width to height
    }
}
