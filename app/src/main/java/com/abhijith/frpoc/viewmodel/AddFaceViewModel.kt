package com.abhijith.frpoc.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abhijith.frpoc.helper.FaceNet
import com.abhijith.frpoc.helper.MediaPipeFaceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddFaceViewModel(
    private val faceNet: FaceNet,
    private val faceDetector: MediaPipeFaceDetector
) : ViewModel() {

    val croppedFaceBitmap = mutableStateOf<Bitmap?>(null)
    val faceEmbedding = mutableStateOf<FloatArray?>(null)
    val errorMessage = mutableStateOf<String?>(null)



    fun processImage(uri: Uri) {
        viewModelScope.launch {
            try {
                // Get the cropped face from the image
                val result = faceDetector.getCroppedFace(uri)
                if (result.isSuccess) {
                    val bitmap = result.getOrNull()
                    croppedFaceBitmap.value = bitmap

                    // Get the face embedding
                    if (bitmap != null) {
                        val embedding = withContext(Dispatchers.Default) {
                            faceNet.getFaceEmbedding(bitmap)
                        }
                        faceEmbedding.value = embedding
                    } else {
                        errorMessage.value = "Failed to crop the face."
                    }
                } else {
                    errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to detect face."
                }
            } catch (e: Exception) {
                errorMessage.value = e.message
            }
        }
    }
}