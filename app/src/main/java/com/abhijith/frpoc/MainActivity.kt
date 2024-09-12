package com.abhijith.frpoc

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.abhijith.frpoc.ui.AddFaceScreen
import com.abhijith.frpoc.ui.DetectScreen
import com.abhijith.frpoc.ui.FaceListScreen
import com.abhijith.frpoc.ui.theme.FRPOCTheme
import com.abhijith.frpoc.viewmodel.AddFaceScreenViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ObjectBoxStore.init(this)
        val viewModel = AddFaceScreenViewModel(context = applicationContext)
        setContent {
            FRPOCTheme {
                val navController = rememberNavController()
                NavigationComponent(applicationContext, navController, viewModel)
            }
        }
    }
}

@Composable
fun NavigationComponent(
    context: Context,
    navController: NavHostController,
    viewModel: AddFaceScreenViewModel
) {
    NavHost(navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("register") { AddFaceScreen(viewModel) { navController.navigateUp() } }
        composable("recognize") { DetectScreen(context) { navController.navigateUp() } }
        composable("faceList") {
            FaceListScreen(context = context, onNavigateBack = { navController.navigateUp() }) {
                navController.navigate("faceList")
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
