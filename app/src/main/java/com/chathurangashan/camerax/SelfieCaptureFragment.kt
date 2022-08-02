package com.chathurangashan.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.chathurangashan.camerax.databinding.FragmentSelfieCaptureBinding
import com.google.android.material.snackbar.Snackbar

class SelfieCaptureFragment : Fragment(R.layout.fragment_selfie_capture) {

    private lateinit var viewBinding: FragmentSelfieCaptureBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private val navigationController: NavController by lazy {
        Navigation.findNavController(requireView())
    }
    var cameraCutoutPivotX = 0f
    var cameraCutoutPivotY = 0f

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewBinding = FragmentSelfieCaptureBinding.bind(view)

        initialization()
        onClickCameraFabButton()
    }

    private fun initialization() {

        viewBinding.root.waitForLayout {

            viewBinding.cameraCutoutView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

            val circleCutoutRadius = resources.getDimension(R.dimen.circle_cut_out_radius)
            val marginFromCircleCutout =
                resources.getDimension(R.dimen.content_margin_from_circle_cutout)
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

        checkCameraPermission()

        viewBinding.cameraCutoutView.waitForLayout {
            cameraCutoutPivotX = viewBinding.cameraCutoutView.pivotX
            cameraCutoutPivotY = viewBinding.cameraCutoutView.pivotY
        }

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
            navigationController.navigate(R.id.to_selfie_preview)
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

    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewBinding.cameraView.surfaceProvider)
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(requireContext()))

    }

    companion object {

        private val TAG = SelfieCaptureFragment::class.simpleName
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