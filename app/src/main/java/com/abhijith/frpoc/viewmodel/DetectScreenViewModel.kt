package com.abhijith.frpoc.viewmodel

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.abhijith.frpoc.database.ImagesVectorDB
import com.abhijith.frpoc.database.PersonDB
import com.abhijith.frpoc.database.RecognitionMetrics
import com.abhijith.frpoc.helper.FaceNet
import com.abhijith.frpoc.helper.ImageVectorUseCase
import com.abhijith.frpoc.helper.MediaPipeFaceDetector
import com.abhijith.frpoc.helper.PersonUseCase

class DetectScreenViewModel(context: Context) : ViewModel() {

    private val personDB: PersonDB = PersonDB()
    private val imagesVectorDB: ImagesVectorDB = ImagesVectorDB()
    private val faceNet: FaceNet = FaceNet(context)
    private val mediaPipeFaceDetector: MediaPipeFaceDetector = MediaPipeFaceDetector(context)
     val imageVectorUseCase: ImageVectorUseCase = ImageVectorUseCase(
        mediapipeFaceDetector = mediaPipeFaceDetector,
        imagesVectorDB = imagesVectorDB,
        faceNet = faceNet
    )
    private val personUseCase: PersonUseCase = PersonUseCase(personDB) // Initialize PersonUseCase with PersonDB

    val faceDetectionMetricsState = mutableStateOf<RecognitionMetrics?>(null)

    fun getNumPeople(): Long = personUseCase.getCount()
}
