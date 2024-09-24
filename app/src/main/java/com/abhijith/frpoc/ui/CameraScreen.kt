package com.abhijith.frpoc.ui

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.abhijith.frpoc.R
import com.abhijith.frpoc.components.AnimatedLine
import com.abhijith.frpoc.viewmodel.AddFaceScreenViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.util.concurrent.Executors

import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraScreen(navController: NavController, viewModel: AddFaceScreenViewModel) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isFaceDetected by remember { mutableStateOf(false) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
    var photoCount by remember { mutableIntStateOf(0) }

    val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }

    CameraPermissionHandler {
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProvider = cameraProviderFuture.get()  // Get the camera provider once.

        val previewView = remember { androidx.camera.view.PreviewView(context) } // Create PreviewView here

        DisposableEffect(Unit) {
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysisUseCase ->
                    analysisUseCase.setAnalyzer(executor) { imageProxy ->
                        processImageProxy(imageProxy, faceDetector) { faceFound ->
                            isFaceDetected = faceFound
                            imageProxy.close()
                        }
                    }
                }

            try {
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

            onDispose {
                cameraProvider.unbindAll()
            }
        }

        // Return the PreviewView as part of AndroidView
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { previewView }, // Only previewView should be returned here
                modifier = Modifier.fillMaxSize()
            )
            OverlayComponent()

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = {
                        imageCapture?.let { capture ->
                            val photoFile = File(
                                context.cacheDir,
                                "${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions =
                                ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            capture.takePicture(
                                outputOptions,
                                executor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onError(exc: ImageCaptureException) {
                                        Log.e("CameraX", "Photo capture failed: ${exc.message}")
                                    }

                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        val uri = Uri.fromFile(photoFile)
                                        saveUriToCache(context, uri)
                                        photoCount += 1
                                        Log.i("AbhijithPhotoURI", "Photo captured: $uri")

                                        if (photoCount == 3) {
                                            Handler(Looper.getMainLooper()).post {
                                                navController.navigate("registerScreen")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                    enabled = isFaceDetected
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.camera_button),
                        contentDescription = "Capture",
                        modifier = Modifier
                            .size(60.dp)
                            .fillMaxSize()
                    )
                }

                Text("$photoCount photos taken", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}


/**
 * Processes the image from the camera and detects faces using ML Kit.
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    faceDetector: FaceDetector,
    onFaceDetected: (Boolean) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                onFaceDetected(faces.isNotEmpty())
            }
            .addOnFailureListener { e ->
                Log.e("FaceDetection", "Face detection failed", e)
                onFaceDetected(false)
            }
    }
}



/**
 * Save the URI in the cache using SharedPreferences or another caching mechanism.
 */
fun saveUriToCache(context: Context, uri: Uri) {
    val sharedPreferences = context.getSharedPreferences("photo_cache", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    val uris = sharedPreferences.getStringSet("photoUris", mutableSetOf()) ?: mutableSetOf()

    uris.add(uri.toString())
    editor.putStringSet("photoUris", uris)
    editor.apply()
}



@Composable
fun OverlayComponent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Image(
                painter = painterResource(id = R.drawable.overlay_camera),
                contentDescription = "Overlay"
            )
            AnimatedLine()
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionHandler(content: @Composable () -> Unit) {
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    when {
        cameraPermissionState.status.isGranted -> {
            content()
        }

        cameraPermissionState.status.shouldShowRationale -> {
            // Show rationale for needing the camera and prompt for permission
            Text("The camera is needed to capture photos.")
            Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                Text("Grant Camera Permission")
            }
        }

        !cameraPermissionState.status.isGranted -> {
            Column {
                Text("Camera permission is required to use this feature.")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("Request Permission")
                }
            }
        }
    }
}
