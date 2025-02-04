package com.abhijith.frpoc.helper

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import com.abhijith.frpoc.database.FaceImageRecord
import com.abhijith.frpoc.database.ImagesVectorDB
import com.abhijith.frpoc.database.RecognitionMetrics
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

class ImageVectorUseCase(
    private val mediapipeFaceDetector: MediaPipeFaceDetector,
    private val imagesVectorDB: ImagesVectorDB,
    private val faceNet: FaceNet
) {

//    private val mediapipeFaceDetector: MediaPipeFaceDetector = MediaPipeFaceDetector(context) // Replace with actual initialization
//    private val imagesVectorDB: ImagesVectorDB = ImagesVectorDB() // Replace with actual initialization
//    private val faceNet: FaceNet = FaceNet() // Replace with actual initialization

    // Add the person's image to the database
    suspend fun addImage(personID: Long, personName: String, imageUri: Uri): Result<Boolean> {
        // Perform face-detection and get the cropped face as a Bitmap
        val faceDetectionResult = mediapipeFaceDetector.getCroppedFace(imageUri)
        if (faceDetectionResult.isSuccess) {
            // Get the embedding for the cropped face, and store it
            // in the database, along with `personId` and `personName`
            val embedding = faceNet.getFaceEmbedding(faceDetectionResult.getOrNull()!!)
            imagesVectorDB.addFaceImageRecord(
                FaceImageRecord(
                    personID = personID,
                    personName = personName,
                    faceEmbedding = embedding
                )
            )
            return Result.success(true)
        } else {
            return Result.failure(faceDetectionResult.exceptionOrNull()!!)
        }
    }

    // From the given frame, return the name of the person by performing
    // face recognition
    suspend fun getNearestPersonName(
        frameBitmap: Bitmap
    ): Pair<RecognitionMetrics?, List<Pair<String, Rect>>> {
        // Perform face-detection and get the cropped face as a Bitmap
        val (faceDetectionResult, t1) =
            measureTimedValue { mediapipeFaceDetector.getAllCroppedFaces(frameBitmap) }
        val faceRecognitionResults = ArrayList<Pair<String, Rect>>()
        var avgT2 = 0L
        var avgT3 = 0L
        for (result in faceDetectionResult) {
            // Get the embedding for the cropped face (query embedding)
            val (croppedBitmap, boundingBox) = result
            val (embedding, t2) = measureTimedValue { faceNet.getFaceEmbedding(croppedBitmap) }
            avgT2 += t2.toLong(DurationUnit.MILLISECONDS)
            // Perform nearest-neighbor search
            val (recognitionResult, t3) =
                measureTimedValue { imagesVectorDB.getNearestEmbeddingPersonName(embedding) }
            avgT3 += t3.toLong(DurationUnit.MILLISECONDS)
            if (recognitionResult == null) {
                faceRecognitionResults.add(Pair("Not recognized", boundingBox))
                continue
            }
            // Calculate cosine similarity between the nearest-neighbor
            // and the query embedding
            val distance = cosineDistance(embedding, recognitionResult.faceEmbedding)
            // If the distance > 0.4, we recognize the person
            // else we conclude that the face does not match enough
            if (distance > 0.7) {
                faceRecognitionResults.add(Pair(recognitionResult.personName, boundingBox))
            } else {
                faceRecognitionResults.add(Pair("Not recognized", boundingBox))
            }
        }
        val metrics =
            if (faceDetectionResult.isNotEmpty()) {
                RecognitionMetrics(
                    timeFaceDetection = t1.toLong(DurationUnit.MILLISECONDS),
                    timeFaceEmbedding = avgT2 / faceDetectionResult.size,
                    timeVectorSearch = avgT3 / faceDetectionResult.size
                )
            } else {
                null
            }

        return Pair(metrics, faceRecognitionResults)
    }

    private fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Double {
        val dotProduct = embedding1.zip(embedding2).sumOf { (e1, e2) -> (e1 * e2).toDouble() }
        val norm1 = sqrt(embedding1.sumOf { (it * it).toDouble() })
        val norm2 = sqrt(embedding2.sumOf { (it * it).toDouble() })
        return dotProduct / (norm1 * norm2)
    }

    private fun cosineDistance(x1: FloatArray, x2: FloatArray): Float {
        var mag1 = 0.0f
        var mag2 = 0.0f
        var product = 0.0f
        for (i in x1.indices) {
            mag1 += x1[i].pow(2)
            mag2 += x2[i].pow(2)
            product += x1[i] * x2[i]
        }
        mag1 = sqrt(mag1)
        mag2 = sqrt(mag2)
        return product / (mag1 * mag2)
    }

    fun removeImages(personID: Long) {
        imagesVectorDB.removeFaceRecordsWithPersonID(personID)
    }
}