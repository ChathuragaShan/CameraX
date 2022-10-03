package com.chathurangashan.camerax

import android.graphics.*
import android.util.Log
import android.util.Size
import android.view.SurfaceHolder
import com.chathurangashan.camerax.interfaces.ObjectInBoundCheck

class FaceDetectionOverlaySurfaceHolder(val mirrorCoordinates: Boolean) : SurfaceHolder.Callback {

    private lateinit var drawingThread: DrawingThread
    lateinit var objectInBoundCheck: ObjectInBoundCheck

    var surfaceTop: Int = 0
    var analyzedImageSize = Size(0, 0)
    var previewScreenSize = Size(0, 0)

    var safeAreaBound = RectF(0f, 0f, 0f, 0f)
    var objectBound: Rect = Rect(0, 0, 0, 0)
    var clearCanvas = false

    companion object {
        private val TAG = FaceDetectionOverlaySurfaceHolder::class.java.simpleName
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        drawingThread = DrawingThread(holder)
        drawingThread.running = true
        drawingThread.start()
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        var retry = true
        drawingThread.running = false

        while (retry) {
            try {
                drawingThread.join()
                retry = false
            } catch (e: InterruptedException) {
            }
        }
    }

    inner class DrawingThread(private val holder: SurfaceHolder?) : Thread() {

        var running = false

        private fun drawAdjustedPreviewSize(canvas: Canvas){

            val myPaint = Paint()
            myPaint.color = Color.rgb(220, 249, 10)
            myPaint.strokeWidth = 5f
            myPaint.style = Paint.Style.STROKE

            if(analyzedImageSize.width != 0 && analyzedImageSize.height != 0) {

                val horizontalScaleFactor = previewScreenSize.width / analyzedImageSize.width.toFloat()
                val verticalScaleFactor = previewScreenSize.height / analyzedImageSize.height.toFloat()

                val adjustedBoundRect = RectF()
                adjustedBoundRect.top = (objectBound.top * verticalScaleFactor) + surfaceTop.toFloat()
                adjustedBoundRect.left = objectBound.right * horizontalScaleFactor
                adjustedBoundRect.right = objectBound.left * horizontalScaleFactor
                adjustedBoundRect.bottom = (objectBound.bottom * verticalScaleFactor) + surfaceTop.toFloat()

                val adjustedMirrorObjectBound = RectF(adjustedBoundRect)

                if(mirrorCoordinates){
                    val originalRight = adjustedBoundRect.right
                    val originalLeft = adjustedBoundRect.left
                    //mirror the coordination since it's the front facing camera
                    adjustedMirrorObjectBound.left = (previewScreenSize.width - originalRight)
                    adjustedMirrorObjectBound.right = (previewScreenSize.width - originalLeft)
                }

                canvas.drawRect(adjustedMirrorObjectBound, myPaint)
            }
        }

        override fun run() {

            while (running) {

                val canvas = holder!!.lockCanvas()

                if (canvas != null) {

                    synchronized(holder) {

                        if (!clearCanvas) {
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                            drawAdjustedPreviewSize(canvas)

                        } else {
                            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                            clearCanvas = false
                        }
                    }

                    holder.unlockCanvasAndPost(canvas)

                } else {
                    Log.e(TAG, "Cannot draw onto the canvas as it's null")
                }

                try {
                    sleep(30)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        }
    }
}