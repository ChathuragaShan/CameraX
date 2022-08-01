package com.chathurangashan.camerax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.chathurangashan.camerax.databinding.FragmentSelfieCaptureBinding
import com.google.android.material.snackbar.Snackbar


class SelfieCaptureFragment : Fragment(R.layout.fragment_selfie_capture) {

    private lateinit var viewBinding: FragmentSelfieCaptureBinding
    protected lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
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

        viewBinding.root.waitForLayout{

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

        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                viewBinding.cameraView.visibility = View.VISIBLE
                //setupCameraIdWithView()
            } else {
                // Permission request was denied.
                Snackbar.make(
                    viewBinding.root,
                    R.string.camera_permission_denied, Snackbar.LENGTH_SHORT
                ).setAction(R.string.ok) { navigationController.navigateUp() }.show()

            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            //setupCameraIdWithView()
        } else {
            viewBinding.cameraView.visibility = View.GONE
            requestCameraPermission()
        }

        viewBinding.cameraCutoutView.waitForLayout {
            cameraCutoutPivotX = viewBinding.cameraCutoutView.pivotX
            cameraCutoutPivotY = viewBinding.cameraCutoutView.pivotY
        }
    }

    private fun onClickCameraFabButton(){

        viewBinding.cameraFabButton.setOnClickListener {
            navigationController.navigate(R.id.to_selfie_preview)
        }
    }

    /**
     * Request camera permission if it is not given
     */
    private fun requestCameraPermission() {

        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            Snackbar.make(viewBinding.root, R.string.camera_access_required, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.ok) {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }.show()
        } else {
            // You can directly ask for the permission.
            Snackbar.make(
                viewBinding.root,
                R.string.camera_permission_not_available,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok) {
                    requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                }.show()
        }

    }

}