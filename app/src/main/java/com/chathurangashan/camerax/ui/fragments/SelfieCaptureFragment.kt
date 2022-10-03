package com.chathurangashan.camerax.ui.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.chathurangashan.camerax.FaceDetectionOverlaySurfaceHolder
import com.chathurangashan.camerax.R
import com.chathurangashan.camerax.SelfieImageAnalyzer
import com.chathurangashan.camerax.databinding.FragmentSelfieCaptureBinding
import com.chathurangashan.camerax.interfaces.SelfieDetectListener
import com.chathurangashan.camerax.waitForLayout
import com.google.android.material.snackbar.Snackbar
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class SelfieCaptureFragment : Fragment(R.layout.fragment_selfie_capture) {

    private lateinit var viewBinding: FragmentSelfieCaptureBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCaptureUseCase: ImageCapture
    private lateinit var faceDetector: FaceDetector
    private lateinit var faceDetectorOptions: FaceDetectorOptions
    private lateinit var selfieDetectListener: SelfieDetectListener
    private var isDrawingDetectedFaceBoundBox = false
    private val overlaySurfaceHolder =  FaceDetectionOverlaySurfaceHolder(true)
    private val navigationController: NavController by lazy {
        Navigation.findNavController(requireView())
    }
    var cameraCutoutPivotX = 0f
    var cameraCutoutPivotY = 0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding = FragmentSelfieCaptureBinding.bind(view)

        initialization()
        onImageIsAnalyzed()
        onClickCameraFabButton()
    }

    private fun initialization() {

        viewBinding.root.waitForLayout {

            viewBinding.cameraCutoutView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            val circleCutoutRadius = resources.getDimension(R.dimen.circle_cut_out_radius)
            val marginFromCircleCutout = resources.getDimension(R.dimen.content_margin_from_circle_cutout)
            val marginToContent = circleCutoutRadius + marginFromCircleCutout

            val descriptionLayoutParams = viewBinding.descriptionTextView.layoutParams
                    as ConstraintLayout.LayoutParams
            descriptionLayoutParams.bottomMargin = marginToContent.toInt()
            viewBinding.descriptionTextView.layoutParams = descriptionLayoutParams

            val documentTypeLayoutParams = viewBinding.documentTypeTextView.layoutParams
                    as ConstraintLayout.LayoutParams
            documentTypeLayoutParams.topMargin = marginToContent.toInt()
            viewBinding.documentTypeTextView.layoutParams = documentTypeLayoutParams

            viewBinding.dynamicalyAlineGroup.visibility = View.VISIBLE
        }

        viewBinding.cameraCutoutView.waitForLayout {
            cameraCutoutPivotX = viewBinding.cameraCutoutView.pivotX
            cameraCutoutPivotY = viewBinding.cameraCutoutView.pivotY
        }

        cameraExecutor = Executors.newSingleThreadExecutor()

        faceDetectorOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .build()

        faceDetector = FaceDetection.getClient(faceDetectorOptions)

        viewBinding.cameraOverlayView.setZOrderMediaOverlay(true)
        val cameraOverlaySurfaceHolder = viewBinding.cameraOverlayView.holder
        cameraOverlaySurfaceHolder.addCallback(overlaySurfaceHolder)
        cameraOverlaySurfaceHolder.setFormat(PixelFormat.TRANSLUCENT)

        checkCameraPermission()

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {

            it.entries.forEach { permission ->

                when (permission.key) {

                    Manifest.permission.CAMERA -> {

                        if (!permission.value) {
                            Snackbar.make(
                                viewBinding.root,
                                R.string.camera_permission_denied, Snackbar.LENGTH_SHORT
                            ).setAction(R.string.ok) {
                                requireActivity().finish()
                            }.show()
                        }
                    }
                    Manifest.permission.WRITE_EXTERNAL_STORAGE -> {

                        if (!permission.value) {
                            Snackbar.make(
                                viewBinding.root,
                                R.string.write_external_storage_denied, Snackbar.LENGTH_SHORT
                            ).setAction(R.string.ok) {
                                requireActivity().finish()
                            }.show()
                        }
                    }
                }
            }

            checkCameraPermission()
        }

    }

    private fun onClickCameraFabButton() {

        viewBinding.cameraFabButton.setOnClickListener {
            takePhoto()
        }
    }

    /**
     * This function is responsible for checking all the permission to start camera. if all permissions
     * are fulfilled it will start the camera preview.
     */
    private fun checkCameraPermission() {

        val allPermissionStatus = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionStatus) {
            startCamera()
        } else {
            requestCameraPermission()
        }
    }

    /**
     * Request camera permission if it is not given
     */
    private fun requestCameraPermission() {

        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) ||
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        ) {
            Snackbar.make(
                viewBinding.root,
                R.string.camera_access_required,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok) {
                    requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
                }.show()

        } else {
            Snackbar.make(
                viewBinding.root,
                R.string.camera_permission_not_available,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok) {
                    requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
                }.show()
        }
    }

    private fun onImageIsAnalyzed(){

        selfieDetectListener = object : SelfieDetectListener {

            override fun onDetectSelfie(selfieFace: Face,proxyImageSize: Size) {

                overlaySurfaceHolder.analyzedImageSize = proxyImageSize

                if(!isDrawingDetectedFaceBoundBox){
                    isDrawingDetectedFaceBoundBox = true

                    val boundingBox = selfieFace.boundingBox

                    Log.d("SelfieCaptureFragment","(${boundingBox.top},${boundingBox.right},${boundingBox.bottom},${boundingBox.left}")

                    overlaySurfaceHolder.objectBound = boundingBox
                    viewBinding.cameraOverlayView.invalidate()
                    isDrawingDetectedFaceBoundBox = false
                }
            }

            override fun onDetectSelfieError(errorMessage: String) {
                Log.d("SelfieCaptureFragment",errorMessage)
            }

        }
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val previewUseCase = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.cameraView.surfaceProvider)
                }

            imageCaptureUseCase = ImageCapture.Builder().build()

            val selfieImageAnalyzer = SelfieImageAnalyzer(faceDetector,selfieDetectListener)

            val imageAnalyzerUseCase = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, selfieImageAnalyzer)
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, previewUseCase, imageCaptureUseCase,imageAnalyzerUseCase
                )

                overlaySurfaceHolder.previewScreenSize =
                    Size(viewBinding.cameraView.width, viewBinding.cameraView.height)

                overlaySurfaceHolder.surfaceTop = viewBinding.cameraView.top

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))

    }

    private fun takePhoto() {

        if(::imageCaptureUseCase.isInitialized){

            val imageCapture = imageCaptureUseCase

            val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis())
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
                }
            }

            val outputOptions = ImageCapture.OutputFileOptions
                .Builder(requireContext().contentResolver,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues)
                .build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                    }

                    override fun
                            onImageSaved(output: ImageCapture.OutputFileResults){
                        val msg = "Photo capture succeeded: ${output.savedUri}"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)
                        navigationController.navigate(R.id.to_selfie_preview)
                    }
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }

    companion object {

        private val TAG = SelfieCaptureFragment::class.simpleName
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}