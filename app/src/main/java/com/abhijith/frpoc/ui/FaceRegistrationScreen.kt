package com.abhijith.frpoc.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.abhijith.frpoc.R
import com.abhijith.frpoc.database.AppDatabase
import com.abhijith.frpoc.database.UserFace
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel


@Composable
fun FaceRegistrationScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context) }
    val appDatabase = AppDatabase.getDatabase(context)
    var imageCapture by remember { mutableStateOf(ImageCapture.Builder().build()) }
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as ComponentActivity,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (exc: Exception) {
            Log.e("CameraX", "Use case binding failed", exc)
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val (cameraViewRef, nameInputRef, buttonRef) = createRefs()

        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .constrainAs(cameraViewRef) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        val nameState = remember { mutableStateOf(TextFieldValue()) }
        OutlinedTextField(
            colors = OutlinedTextFieldDefaults.colors(),
            value = nameState.value,
            onValueChange = { nameState.value = it },
            label = { Text("Enter Name") },
            modifier = Modifier
                .fillMaxWidth()
                .constrainAs(nameInputRef) {
                    top.linkTo(cameraViewRef.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        Button(
            onClick = {
                val name = nameState.value.text.trim()
                if (name.isNotEmpty()) {
                    capturePhoto(name, appDatabase, imageCapture, coroutineScope, context)
                } else {
                    Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.constrainAs(buttonRef) {
                top.linkTo(nameInputRef.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            Text("Capture and Save")
        }
    }
}

private fun capturePhoto(
    userName: String,
    appDatabase: AppDatabase,
    imageCapture: ImageCapture,
    coroutineScope: CoroutineScope,
    context: Context
) {
    // Create a temporary file to save the full image
    val photoFile = File(context.cacheDir, "$userName.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("CameraX", "Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                // Decode the captured image file to a Bitmap
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                // Launch a coroutine to process the image asynchronously
                coroutineScope.launch {
                    val croppedFace = detectAndCropFaceUsingMLKit(context, bitmap)
                    if (croppedFace != null) {
                        // Save the cropped face image
                        val croppedPhotoFile = File(context.getOutputDirectory(), "$userName.jpg")
                        saveBitmapToFile(croppedFace, croppedPhotoFile)

                        // Extract embeddings from the cropped grayscale face
                        val embeddings = processCapturedImage(context, croppedFace)
                        if (embeddings != null) {
                            // Save face data if embeddings are successfully processed
                            saveFaceData(
                                userName,
                                croppedPhotoFile,
                                embeddings,
                                appDatabase,
                                coroutineScope
                            )
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Cropped face saved and data processed",
                                    Toast.LENGTH_SHORT
                                ).show()

                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Failed to process image",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Cropped face value - null", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }

        }
    )
}

private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
    }
}

private suspend fun processCapturedImage(context: Context, croppedFace: Bitmap): FloatArray? {
    // Convert the cropped face to grayscale
    val grayscaleBitmap = convertToGrayscale(croppedFace)

    // Prepare the grayscale bitmap for embedding extraction
    val model = Interpreter(loadModelFile(context))
    val width = 160
    val height = 160
    val resizedGrayscaleFace = Bitmap.createScaledBitmap(grayscaleBitmap, width, height, true)
    val inputBuffer = convertBitmapToByteBuffer(resizedGrayscaleFace, height)

    // Run the model to get embeddings
    val output = Array(1) { FloatArray(512) }
    model.run(inputBuffer, output)
    return output[0]
}



private suspend fun detectAndCropFaceUsingMLKit(context: Context, bitmap: Bitmap): Bitmap? {
    return withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.1f) // Example value; adjust as needed
            .enableTracking() // Enable face tracking for better detection
            .build()

        val detector = FaceDetection.getClient(options)
        var croppedFace: Bitmap? = null

        try {
            val faces = detector.process(image).await()

            if (faces.isNotEmpty()) {
                // Example: Handle multiple faces and select the most centered one
                val largestFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                val bounds = largestFace?.boundingBox ?: return@withContext null

                val left = maxOf(bounds.left, 0)
                val top = maxOf(bounds.top, 0)
                val right = minOf(bounds.right, bitmap.width)
                val bottom = minOf(bounds.bottom, bitmap.height)

                if (right > left && bottom > top) {
                    croppedFace = Bitmap.createBitmap(
                        bitmap,
                        left,
                        top,
                        right - left,
                        bottom - top
                    )

                    // Align face if necessary
                    // Example: Apply face alignment using landmarks (if available)
                } else {
                    Log.d("FaceDetection", "Detected face with invalid dimensions")
                }
            } else {
                Log.d("FaceDetection", "No faces detected")
            }
        } catch (e: Exception) {
            Log.e("FaceDetection", "Error processing face detection", e)
        }

        croppedFace
    }
}


private fun convertToGrayscale(bitmap: Bitmap): Bitmap {
    val grayscaleBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayscaleBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix().apply {
        setSaturation(0f)
    }
    val colorFilter = ColorMatrixColorFilter(colorMatrix)
    paint.colorFilter = colorFilter
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return grayscaleBitmap
}


private fun saveFaceData(
    userName: String,
    photoFile: File,
    embeddings: FloatArray,
    appDatabase: AppDatabase,
    coroutineScope: CoroutineScope
) {
    val embeddingsString = embeddings.joinToString(",")

    coroutineScope.launch(Dispatchers.IO) {
        appDatabase.userFaceDao()
            .insert(
                UserFace(
                    name = userName,
                    embedding = embeddingsString,
                    photoPath = photoFile.absolutePath,
                    isDeleted = 0
                )
            )
    }
}

private fun android.content.Context.getOutputDirectory(): File {
    val mediaDir = externalMediaDirs.firstOrNull()?.let {
        File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
    }
    return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir
}

private fun loadModelFile(context: Context): ByteBuffer {
    val assetFileDescriptor = context.assets.openFd(context.getString(R.string.my_model_512))
    val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = assetFileDescriptor.startOffset
    val declaredLength = assetFileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}


fun convertBitmapToByteBuffer(bitmap: Bitmap, inputSize: Int): ByteBuffer {
    // 1 float = 4 bytes, 3 channels (RGB)
    val byteBuffer = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3)
    byteBuffer.order(ByteOrder.nativeOrder())

    val intValues = IntArray(inputSize * inputSize)
    bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    var pixel = 0
    for (i in 0 until inputSize) {
        for (j in 0 until inputSize) {
            val pixelValue = intValues[pixel++]

            // Extract RGB values
            val r = (pixelValue shr 16 and 0xFF).toFloat()
            val g = (pixelValue shr 8 and 0xFF).toFloat()
            val b = (pixelValue and 0xFF).toFloat()

            // Normalize the pixel values to [0, 1]
            byteBuffer.putFloat(r / 255.0f)
            byteBuffer.putFloat(g / 255.0f)
            byteBuffer.putFloat(b / 255.0f)
        }
    }

    return byteBuffer
}



