package com.chathurangashan.camerax.interfaces

import android.util.Size
import com.google.mlkit.vision.face.Face

interface SelfieDetectListener {
    fun onDetectSelfie(selfieFace: Face, proxyImageSize: Size)
    fun onDetectSelfieError(errorMessage: String)
}