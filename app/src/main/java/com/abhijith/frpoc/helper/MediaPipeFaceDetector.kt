package com.abhijith.frpoc.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.net.Uri
import androidx.core.graphics.toRect
import androidx.exifinterface.media.ExifInterface
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaPipeFaceDetector(private val context: Context) {

    // Model file name from the assets folder
    private val modelName = "blaze_face_short_range.tflite"

    // Initialize the base options and face detector
    private val baseOptions = BaseOptions.builder()
        .setModelAssetPath(modelName)
        .build()

    private val faceDetectorOptions = FaceDetector.FaceDetectorOptions.builder()
        .setBaseOptions(baseOptions)
        .setRunningMode(RunningMode.IMAGE)
        .build()

    private val faceDetector = FaceDetector.createFromOptions(context, faceDetectorOptions)

    // Get a cropped face from the image URI
    suspend fun getCroppedFace(imageUri: Uri): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val imageBitmap = loadImageWithExifRotation(imageUri) ?: return@withContext Result.failure<Bitmap>(
                AppException(ErrorCode.FACE_DETECTOR_FAILURE)
            )

            // Detect faces
            val faces = faceDetector.detect(BitmapImageBuilder(imageBitmap).build()).detections()

            if (faces.size > 1) {
                return@withContext Result.failure(AppException(ErrorCode.MULTIPLE_FACES))
            } else if (faces.isEmpty()) {
                return@withContext Result.failure(AppException(ErrorCode.NO_FACE))
            }

            // Crop the face
            val faceRect = faces[0].boundingBox().toRect()
            if (validateRect(imageBitmap, faceRect)) {
                val croppedBitmap = Bitmap.createBitmap(imageBitmap, faceRect.left, faceRect.top, faceRect.width(), faceRect.height())
                return@withContext Result.success(croppedBitmap)
            } else {
                return@withContext Result.failure(AppException(ErrorCode.FACE_DETECTOR_FAILURE))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(AppException(ErrorCode.FACE_DETECTOR_FAILURE))
        }
    }

    // Detect multiple faces and return a list of cropped faces and their bounding boxes
    suspend fun getAllCroppedFaces(imageBitmap: Bitmap): List<Pair<Bitmap, Rect>> = withContext(Dispatchers.IO) {
        val faces = faceDetector.detect(BitmapImageBuilder(imageBitmap).build()).detections()
        faces.filter { validateRect(imageBitmap, it.boundingBox().toRect()) }
            .map { detection ->
                val rect = detection.boundingBox().toRect()
                val croppedBitmap = Bitmap.createBitmap(imageBitmap, rect.left, rect.top, rect.width(), rect.height())
                croppedBitmap to rect
            }
    }

    // Load the image from the URI and apply EXIF-based rotation
    private fun loadImageWithExifRotation(imageUri: Uri): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(imageUri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val exifInputStream = context.contentResolver.openInputStream(imageUri) ?: return bitmap
        val exifInterface = ExifInterface(exifInputStream)
        val rotation = when (exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        exifInputStream.close()

        return if (rotation != 0f) {
            rotateBitmap(bitmap, rotation)
        } else {
            bitmap
        }
    }

    // Rotate bitmap based on EXIF data
    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    // Validate if the bounding box fits within the image
    private fun validateRect(bitmap: Bitmap, boundingBox: Rect): Boolean {
        return boundingBox.left >= 0 &&
                boundingBox.top >= 0 &&
                (boundingBox.left + boundingBox.width()) <= bitmap.width &&
                (boundingBox.top + boundingBox.height()) <= bitmap.height
    }
}