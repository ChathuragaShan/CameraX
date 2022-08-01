package com.chathurangashan.camerax

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.chathurangashan.camerax.databinding.FragmentSefiePereviewBinding

class SelfiePreviewFragment : Fragment(R.layout.fragment_sefie_pereview) {

    private lateinit var viewBinding: FragmentSefiePereviewBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewBinding = FragmentSefiePereviewBinding.bind(view)

        initialization()
    }

    private fun initialization(){

    }
}