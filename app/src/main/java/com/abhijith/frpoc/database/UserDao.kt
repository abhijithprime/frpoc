package com.abhijith.frpoc.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "user_faces")
data class UserFace(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val embedding: String,  // Store embeddings as a comma-separated string
    val photoPath: String,
    val isDeleted: Int
)


@Dao
interface UserFaceDao {
    @Insert
    suspend fun insert(userFace: UserFace)

    @Query("SELECT * FROM user_faces where isDeleted = 0")
    suspend fun getAllUserFaces(): List<UserFace>

    @Query("SELECT * FROM user_faces WHERE id = :id")
    suspend fun getUserFaceById(id: Int): UserFace?
}
