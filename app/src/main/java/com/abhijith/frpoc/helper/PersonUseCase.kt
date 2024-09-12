package com.abhijith.frpoc.helper

import com.abhijith.frpoc.database.PersonDB
import com.abhijith.frpoc.database.PersonRecord
import kotlinx.coroutines.flow.Flow

class PersonUseCase(private val personDB: PersonDB) {

    fun addPerson(name: String, numImages: Long): Long {
        return personDB.addPerson(
            PersonRecord(
                personName = name,
                numImages = numImages,
                addTime = System.currentTimeMillis()
            )
        )
    }

    fun removePerson(id: Long) {
        personDB.removePerson(id)
    }

    fun getAll(): Flow<List<PersonRecord>> = personDB.getAll()

    fun getCount(): Long = personDB.getCount()
}