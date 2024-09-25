package com.abhijith.frpoc.ui

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.abhijith.frpoc.R
import com.abhijith.frpoc.ui.theme.FRPOCTheme
import com.abhijith.frpoc.ui.theme.claret
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay


@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AddFaceScreen2(navController: NavHostController) {
    FRPOCTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                val permissionsState = rememberMultiplePermissionsState(
                    permissions = listOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                )

                LaunchedEffect(Unit) {
                    // Request permissions on first render
                    permissionsState.launchMultiplePermissionRequest()
                }

                HandlePermissionRequests(permissionsState)

                ScreenUI(navController)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HandlePermissionRequests(permissionsState: MultiplePermissionsState) {
    if (!permissionsState.allPermissionsGranted) {
        LaunchedEffect(Unit) {
            // Keep requesting until permissions are granted
            while (!permissionsState.allPermissionsGranted) {
                delay(500)  // Delay between permission requests
                permissionsState.launchMultiplePermissionRequest()
            }
        }
    }
}

@Composable
private fun ScreenUI(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.BottomCenter
    ) {
        Image(
            painter = painterResource(id = R.drawable.background),
            contentDescription = "background picture",
            modifier = Modifier.fillMaxWidth(1f),
            contentScale = ContentScale.FillBounds
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(bottom = 100.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Face Recognition",
                color = claret,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Attendance",
                color = claret,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            Image(
                painter = painterResource(id = R.drawable.face_register),
                contentDescription = "Face Recognition",
                modifier = Modifier
                    .padding(24.dp)
                    .size(190.dp)
                    .background(color = Color.White, shape = CircleShape)
            )
            Button(
                onClick = {
                    navController.navigate("cameraScreen")
                },
                colors = ButtonDefaults.buttonColors(containerColor = claret),
                modifier = Modifier
                    .padding(top = 32.dp)
                    .height(48.dp)
                    .fillMaxWidth(0.5f)
            ) {
                Text(
                    text = "Register",
                    color = Color.White,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp
                )
            }

//            Button(
//                onClick = {
//                    navController.navigate("recognizeScreen")
//                },
//                colors = ButtonDefaults.buttonColors(containerColor = claret),
//                modifier = Modifier
//                    .padding(top = 32.dp)
//                    .height(48.dp)
//                    .fillMaxWidth(0.5f)
//            ) {
//                Text(
//                    text = "Recognize",
//                    color = Color.White,
//                    fontWeight = FontWeight.Normal,
//                    fontSize = 16.sp
//                )
//            }
        }

    }

}

//Create a preview for the AddFaceScreen2
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Preview(showBackground = true)
@Composable
fun AddFaceScreen2Preview() {
    val navController = rememberNavController()
    AddFaceScreen2(navController)
}




