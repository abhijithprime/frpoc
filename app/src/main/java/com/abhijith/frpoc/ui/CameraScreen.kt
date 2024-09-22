package com.abhijith.frpoc.ui

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScreen(navController: NavController, viewModel: AddFaceScreenViewModel) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    val executor = remember { Executors.newSingleThreadExecutor() }
    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

    // To hold the URI of captured photos
    val photoUris = viewModel.photoUris
    var photoCount by remember { mutableIntStateOf(0) }

    CameraPermissionHandler {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = androidx.camera.view.PreviewView(ctx)
                    val preview = androidx.camera.core.Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder().build()

                    try {
                        cameraProviderFuture.get().bindToLifecycle(
                            ctx as androidx.lifecycle.LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                        Log.e("CameraX", "Use case binding failed", exc)
                    }
                    previewView
                },
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
                                context.externalMediaDirs.first(),
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
                                        photoUris.add(uri)
                                        viewModel.setImageUri(uri.toString())
                                        photoCount += 1
                                        Log.i("AbhijithPhotoURI", "Photo captured: $uri")

                                        // Switch to the main thread for navigation
                                        if (photoCount == 3) {
                                            viewModel.addPhotoUris(photoUris)
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
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.camera_button),
                        contentScale = ContentScale.Fit,
                        contentDescription = "Capture",
                        modifier = Modifier
                            .size(60.dp)
                            .fillMaxSize()
                    )
                }

                // Display the count of photos taken
                if (photoUris.isNotEmpty()) {
                    Text("${photoUris.size} photos taken", color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun OverlayComponent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Image(
                painter = painterResource(id = R.drawable.overlay),
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
