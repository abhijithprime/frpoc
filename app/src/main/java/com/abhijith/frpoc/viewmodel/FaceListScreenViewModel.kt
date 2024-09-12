package com.abhijith.frpoc.viewmodel

import androidx.lifecycle.ViewModel
import com.abhijith.frpoc.helper.ImageVectorUseCase
import com.abhijith.frpoc.helper.PersonUseCase

class FaceListScreenViewModel(
    private val imageVectorUseCase: ImageVectorUseCase,
    private val personUseCase: PersonUseCase
) : ViewModel() {

    val personFlow = personUseCase.getAll()

    // Remove the person from `PersonRecord`
    // and all associated face embeddings from `FaceImageRecord`
    fun removeFace(id: Long) {
        personUseCase.removePerson(id)
        imageVectorUseCase.removeImages(id)
    }
}
