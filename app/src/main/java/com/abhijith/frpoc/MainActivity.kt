package com.abhijith.frpoc

import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.abhijith.frpoc.database.ObjectBoxStore
import com.abhijith.frpoc.ui.AddFaceScreen2
import com.abhijith.frpoc.ui.CameraScreen
import com.abhijith.frpoc.ui.DetectScreen
import com.abhijith.frpoc.ui.FaceListScreen
import com.abhijith.frpoc.ui.RegisterScreen
import com.abhijith.frpoc.ui.theme.FRPOCTheme
import com.abhijith.frpoc.viewmodel.AddFaceScreenViewModel

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ObjectBoxStore.init(this)
        val viewModel = AddFaceScreenViewModel(context = applicationContext)
        viewModel.clearCachedUris(applicationContext)
        setContent {
            NavigationSetup(context = applicationContext, viewModel)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun NavigationSetup(context: Context, viewModel: AddFaceScreenViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "addFaceScreen") {
        composable("addFaceScreen") { AddFaceScreen2(navController) }
        composable("cameraScreen") {
            CameraScreen(
                navController,
                viewModel = AddFaceScreenViewModel(context)
            )
        }
        composable("registerScreen") {
            RegisterScreen(
                viewModel,
                navController
            ) { navController.navigateUp() }
        }
        composable("recognizeScreen") {
            DetectScreen(context = context, navController = navController) {
                navController.navigate("face_list_screen")
            }
        }
        composable("face_list_screen") {
            FaceListScreen(context, { navController.navigateUp() }) {
                navController.navigate("cameraScreen")
            }
        }
    }
}

@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = { navController.navigate("register") }) {
            Text("Register Face")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("recognize") }) {
            Text("Recognize Face")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FRPOCTheme {
        val navController = rememberNavController()
        HomeScreen(navController)
    }
}
