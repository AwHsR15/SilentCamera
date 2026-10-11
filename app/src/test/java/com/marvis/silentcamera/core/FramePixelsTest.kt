package com.marvis.silentcamera.core

import org.junit.Assert.*
import org.junit.Test

class FramePixelsTest {
    @Test fun paddedRowsAndChromaPixelStridesDoNotLeakPadding() {
        val pixels=FramePixels.toArgb(2,2,byteArrayOf(16,16,99,99,235.toByte(),235.toByte(),99,99),4,1,
            byteArrayOf(128.toByte(),77),2,2,byteArrayOf(128.toByte(),88),2,2)
        assertArrayEquals(intArrayOf(0xff000000.toInt(),0xff000000.toInt(),0xffffffff.toInt(),0xffffffff.toInt()),pixels)
    }
    @Test fun undersizedPlanesAndOversizedFramesFailBeforeAllocation() {
        for(size in listOf(2 to 2,10000 to 10000)) {
            try{FramePixels.toArgb(size.first,size.second,byteArrayOf(16),1,1,byteArrayOf(128.toByte()),1,1,byteArrayOf(128.toByte()),1,1);fail()}
            catch(_:IllegalArgumentException){}
        }
    }
    @Test fun sensorRotationSwapsWidthAndHeightOnlyForQuarterTurns() {
        assertEquals(480 to 640,FramePixels.rotatedSize(640,480,90))
        assertEquals(640 to 480,FramePixels.rotatedSize(640,480,180))
        assertEquals(480 to 640,FramePixels.rotatedSize(640,480,270))
        try{FramePixels.rotatedSize(640,480,42);fail()}catch(_:IllegalArgumentException){}
    }
}
