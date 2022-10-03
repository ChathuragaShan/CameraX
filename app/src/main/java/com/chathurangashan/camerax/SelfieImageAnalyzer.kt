package com.chathurangashan.camerax

import android.annotation.SuppressLint
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.chathurangashan.camerax.interfaces.SelfieDetectListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector

class SelfieImageAnalyzer(
    private val faceDetector: FaceDetector,
    private val listener: SelfieDetectListener): ImageAnalysis.Analyzer {

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image
        if (mediaImage != null) {

            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

            val proxyImageSize = when (rotationDegrees) {
                270, 90 -> {
                    Size(image.height,image.width)
                }
                0, 180 -> {
                    Size(image.width,image.height)
                }
                else -> {
                    Size(0,0)
                }
            }
            Log.d("SelfieImageAnalyzer", rotationDegrees.toString())

            faceDetector.process(image)
                .addOnSuccessListener { faces ->
                    if(faces.count() == 1){
                        listener.onDetectSelfie(faces[0],proxyImageSize)
                    }else if (faces.isEmpty()){
                        listener.onDetectSelfieError("No face is detected to capture")
                    }else{
                        listener.onDetectSelfieError("Multiple faces are detected to capture")
                    }

                    imageProxy.close()
                }
                .addOnFailureListener { e ->
                    listener.onDetectSelfieError(e.message.toString())
                    imageProxy.close()
                }
        }
    }
}