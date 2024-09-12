package com.abhijith.frpoc.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.abhijith.frpoc.R
import com.abhijith.frpoc.database.AppDatabase
import com.abhijith.frpoc.database.UserFace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.sqrt

@Composable
fun FaceRecognitionScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val previewView = remember { PreviewView(context) }
    val appDatabase = AppDatabase.getDatabase(context)
    val model = remember { Interpreter(loadModelFile(context)) }
    var recognizedBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

        try {
            cameraProvider.unbindAll()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(160, 160)) // Set an appropriate resolution
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        try {
                            val bitmap = imageProxyToBitmap(imageProxy)
                            val grayscaleBitmap = bitmap?.let { toGrayscale(it) }
                            Log.i("Abhijith", "Grayscale Bitmap: $grayscaleBitmap")
                            val embeddings = grayscaleBitmap?.let { extractEmbeddings(model, it) }
                            Log.i("Abhijith", "Embeddings: $embeddings")
                            if (embeddings != null) {
                                recognizeFaceWithCosineSimilarity(
                                    embeddings,
                                    appDatabase,
                                    coroutineScope,
                                ) { recognizedFace ->
                                    recognizedFace?.let {
                                        recognizedBitmap = BitmapFactory.decodeFile(it.photoPath)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("FaceRecognitionScreen", "Error processing image", e)
                        } finally {
                            imageProxy.close()
                        }
                    }
                }


            cameraProvider.bindToLifecycle(
                context as ComponentActivity,
                cameraSelector,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            Log.e("CameraX", "Use case binding failed", exc)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        recognizedBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val yPlane = image.planes[0].buffer
        val uPlane = image.planes[1].buffer
        val vPlane = image.planes[2].buffer

        // Check buffer sizes
        val ySize = yPlane.remaining()
        val uSize = uPlane.remaining()
        val vSize = vPlane.remaining()

        // Calculate total buffer size
        val bufferSize = ySize + uSize + vSize
        Log.d("imageProxyToBitmap", "Y Size: $ySize, U Size: $uSize, V Size: $vSize")

        if (bufferSize < image.width * image.height * 1.5) {
            Log.e("imageProxyToBitmap", "Buffer size is smaller than expected. Returning null.")
            return null
        }

        // Allocate buffer for NV21 format
        val nv21 = ByteArray(bufferSize)

        // Copy Y plane
        yPlane.get(nv21, 0, ySize)

        // Interleave U and V planes
        var uvOffset = ySize
        for (i in 0 until uSize) {
            nv21[uvOffset++] = vPlane[i]
            nv21[uvOffset++] = uPlane[i]
        }

        // Convert to Bitmap
        yuvToBitmap(nv21, image.width, image.height)
    } catch (e: Exception) {
        Log.e("imageProxyToBitmap", "Error converting ImageProxy to Bitmap", e)
        null
    }
}


private fun yuvToBitmap(yuv: ByteArray, width: Int, height: Int): Bitmap {
    val yuvImage = YuvImage(yuv, ImageFormat.NV21, width, height, null)
    val outputStream = ByteArrayOutputStream()
    yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, outputStream)
    val jpegByteArray = outputStream.toByteArray()
    return BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.size)
}

private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
    val imageMean = 127.5f
    val imageStd = 127.5f
    val byteBuffer =
        ByteBuffer.allocateDirect(4 * bitmap.width * bitmap.height * 3) // 4 bytes per float, 3 channels (RGB)
    byteBuffer.order(ByteOrder.nativeOrder())
    val intValues = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    for (pixel in intValues) {
        // Normalize the pixel values (as in the Java example)
        byteBuffer.putFloat((((pixel shr 16) and 0xFF) - imageMean) / imageStd)  // Red channel
        byteBuffer.putFloat((((pixel shr 8) and 0xFF) - imageMean) / imageStd)   // Green channel
        byteBuffer.putFloat(((pixel and 0xFF) - imageMean) / imageStd)           // Blue channel
    }
    return byteBuffer
}

private fun extractEmbeddings(model: Interpreter, bitmap: Bitmap): FloatArray? {
    val resizedBitmap = resizeBitmap(bitmap, 160, 160) // Resize as per model input size
    Log.i("Abhijith", "Resized Bitmap: ${resizedBitmap.width}x${resizedBitmap.height}")

    val byteBuffer = bitmapToByteBuffer(resizedBitmap) // Now with normalized input
    Log.i("Abhijith", "ByteBuffer size: ${byteBuffer.capacity()}")

    val inputTensorShape = intArrayOf(1, 160, 160, 3) // Shape as per the model's input tensor
    Log.i("Abhijith", "Input tensor shape: ${inputTensorShape.contentToString()}")

    val outputBuffer = TensorBuffer.createFixedSize(
        intArrayOf(1, 512), // Adjust output shape based on the model
        DataType.FLOAT32
    )
    Log.i("Abhijith", "Output buffer created with shape: ${outputBuffer.shape.contentToString()}")

    return try {
        model.run(byteBuffer, outputBuffer.buffer.rewind())  // Run the model inference
        outputBuffer.floatArray
    } catch (e: IllegalArgumentException) {
        Log.e("Abhijith", "Error running the model", e)
        null
    }
}


private fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap {
    return Bitmap.createScaledBitmap(bitmap, width, height, true)
}


private fun loadModelFile(context: Context): MappedByteBuffer {
    val fileDescriptor = context.assets.openFd(context.getString(R.string.my_model_512))
    val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
    val fileChannel = inputStream.channel
    val startOffset = fileDescriptor.startOffset
    val declaredLength = fileDescriptor.declaredLength
    return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
}


private fun cosineSimilarity(embedding1: FloatArray, embedding2: FloatArray): Double {
    val dotProduct = embedding1.zip(embedding2).sumOf { (e1, e2) -> (e1 * e2).toDouble() }
    val norm1 = sqrt(embedding1.sumOf { (it * it).toDouble() })
    val norm2 = sqrt(embedding2.sumOf { (it * it).toDouble() })
    return dotProduct / (norm1 * norm2)
}

private fun recognizeFaceWithCosineSimilarity(
    capturedEmbeddings: FloatArray,
    appDatabase: AppDatabase,
    coroutineScope: CoroutineScope,
    onRecognitionResult: (UserFace?) -> Unit
) {
    coroutineScope.launch(Dispatchers.IO) {
        val userFaces = appDatabase.userFaceDao().getAllUserFaces()
        val recognizedFace = userFaces.find { face ->
            val databaseEmbeddings = face.embedding.split(",").map { it.toFloat() }.toFloatArray()
            val similarity = cosineSimilarity(capturedEmbeddings, databaseEmbeddings)
            Log.i("FaceRecognitionScreen", "Cosine Similarity: $similarity")
            similarity > 0.7F  // Adjust the threshold for your needs
        }


        recognizedFace?.let {
            Log.i("FaceRecognitionScreen", "Recognized person: ${it.name}")
        } ?: run {
            Log.i("FaceRecognitionScreen", "No match found")
        }
        onRecognitionResult(recognizedFace)
    }
}

private fun toGrayscale(bitmap: Bitmap): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    for (y in 0 until height) {
        for (x in 0 until width) {
            val pixel = bitmap.getPixel(x, y)
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            val gray = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
            val grayPixel = (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray
            grayscaleBitmap.setPixel(x, y, grayPixel)
        }
    }

    return grayscaleBitmap
}



