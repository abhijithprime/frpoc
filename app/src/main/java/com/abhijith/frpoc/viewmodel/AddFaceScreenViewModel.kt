package com.abhijith.frpoc.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.abhijith.frpoc.components.setProgressDialogText
import com.abhijith.frpoc.database.ImagesVectorDB
import com.abhijith.frpoc.database.PersonDB
import com.abhijith.frpoc.helper.AppException
import com.abhijith.frpoc.helper.FaceNet
import com.abhijith.frpoc.helper.ImageVectorUseCase
import com.abhijith.frpoc.helper.MediaPipeFaceDetector
import com.abhijith.frpoc.helper.PersonUseCase
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

    private val _imageUri = MutableLiveData<String>()
    val imageUri: LiveData<String> = _imageUri

    // Mutable list to store photo URIs
    val photoUris = mutableStateListOf<Uri>()

    fun setImageUri(uri: String) {
        // Use postValue if updating LiveData from a background thread
        _imageUri.postValue(uri)
    }


    // Function to add new URIs to the list
    fun addPhotoUri(uri: Uri) {
        photoUris.add(uri)
    }

    // Function to add multiple URIs at once (if needed)
    fun addPhotoUris(uris: List<Uri>) {
        photoUris.addAll(uris)
    }

    // Pass the dependencies into the use cases
    private val personUseCase: PersonUseCase = PersonUseCase(personDB)
    private val imageVectorUseCase: ImageVectorUseCase = ImageVectorUseCase(
        mediapipeFaceDetector = mediaPipeFaceDetector,
        imagesVectorDB = imagesVectorDB,
        faceNet = faceNet
    )

    // Mutable states for the UI
    val personNameState: MutableState<String> = mutableStateOf("")
    val personEmailState: MutableState<String> = mutableStateOf("")
    val personPhoneState: MutableState<String> = mutableStateOf("")
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

    /**
     * Retrieve the cached URIs when needed.
     */
    fun getCachedUris(context: Context): Set<String>? {
        val sharedPreferences = context.getSharedPreferences("photo_cache", Context.MODE_PRIVATE)
        return sharedPreferences.getStringSet("photoUris", emptySet())
    }

    fun clearCachedUris(context: Context) {
        val sharedPreferences = context.getSharedPreferences("photo_cache", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("photoUris") // Remove the cached URIs
        editor.apply() // Apply the changes
    }

    fun loadCachedUris(context: Context) {
        val sharedPreferences = context.getSharedPreferences("photo_cache", Context.MODE_PRIVATE)
        val cachedUrisSet = sharedPreferences.getStringSet("photoUris", emptySet())

        // Convert cached string URIs to a List<Uri>
        val uriList = cachedUrisSet?.map { Uri.parse(it) } ?: emptyList()

        // Update the MutableState with the retrieved URIs
        selectedImageURIs.value = uriList
    }

}
