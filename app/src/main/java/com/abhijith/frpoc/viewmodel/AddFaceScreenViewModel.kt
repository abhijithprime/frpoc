package com.abhijith.frpoc.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.abhijith.frpoc.components.setProgressDialogText
import com.abhijith.frpoc.database.ImagesVectorDB
import com.abhijith.frpoc.database.PersonDB
import com.abhijith.frpoc.helper.AppException
import com.abhijith.frpoc.helper.ImageVectorUseCase
import com.abhijith.frpoc.helper.PersonUseCase
import com.abhijith.frpoc.helper.MediaPipeFaceDetector
import com.abhijith.frpoc.helper.FaceNet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddFaceScreenViewModel(context: Context) : ViewModel() {

    // Initialize the dependencies first
    private val personDB: PersonDB
    init {
        personDB = PersonDB()
    }
    private val imagesVectorDB: ImagesVectorDB = ImagesVectorDB()
    private val faceNet: FaceNet = FaceNet(context)
    private val mediaPipeFaceDetector: MediaPipeFaceDetector = MediaPipeFaceDetector(context)

    // Pass the dependencies into the use cases
    private val personUseCase: PersonUseCase = PersonUseCase(personDB)
    private val imageVectorUseCase: ImageVectorUseCase = ImageVectorUseCase(
        mediapipeFaceDetector = mediaPipeFaceDetector,
        imagesVectorDB = imagesVectorDB,
        faceNet = faceNet
    )

    // Mutable states for the UI
    val personNameState: MutableState<String> = mutableStateOf("")
    val selectedImageURIs: MutableState<List<Uri>> = mutableStateOf(emptyList())

    val isProcessingImages: MutableState<Boolean> = mutableStateOf(false)
    val numImagesProcessed: MutableState<Int> = mutableIntStateOf(0)

    // Method to process the images and add them to the database
    fun addImages() {
        isProcessingImages.value = true
        CoroutineScope(Dispatchers.Default).launch {
            // Add the person to the database
            val id = personUseCase.addPerson(
                personNameState.value,
                selectedImageURIs.value.size.toLong()
            )

            // Process each selected image and add them to the vector database
            selectedImageURIs.value.forEach {
                imageVectorUseCase
                    .addImage(id, personNameState.value, it)
                    .onFailure {
                        // Handle failure, display error message
                        val errorMessage = (it as AppException).errorCode.message
                        setProgressDialogText(errorMessage)
                    }
                    .onSuccess {
                        // Update the number of images processed
                        numImagesProcessed.value += 1
                        setProgressDialogText("Processed ${numImagesProcessed.value} image(s)")
                    }
            }

            // Reset the processing flag once done
            isProcessingImages.value = false
        }
    }
}
