package com.abhijith.frpoc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.abhijith.frpoc.helper.FaceNet
import com.abhijith.frpoc.helper.MediaPipeFaceDetector

class AddFaceViewModelFactory(
    private val faceNet: FaceNet,
    private val faceDetector: MediaPipeFaceDetector
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddFaceViewModel::class.java)) {
            return AddFaceViewModel(faceNet, faceDetector) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}