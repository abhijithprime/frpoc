package com.abhijith.frpoc.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.abhijith.frpoc.R
import com.abhijith.frpoc.components.AppProgressDialog
import com.abhijith.frpoc.components.hideProgressDialog
import com.abhijith.frpoc.components.showProgressDialog
import com.abhijith.frpoc.ui.theme.FRPOCTheme
import com.abhijith.frpoc.ui.theme.claret
import com.abhijith.frpoc.viewmodel.AddFaceScreenViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(viewModel: AddFaceScreenViewModel = viewModel(), onNavigateBack: () -> Unit) {

    FRPOCTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Register",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.Gray
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    ),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Navigate Back",
                                tint = Color.Gray
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel)
                ImageReadProgressDialog(viewModel, onNavigateBack)
            }
        }
    }


}

@Composable
private fun ScreenUI(viewModel: AddFaceScreenViewModel) {
    // Observe the photoUris from the ViewModel
    val photoUris = viewModel.photoUris
    val uri = viewModel.imageUri.observeAsState()


    val context = LocalContext.current

    // State for input fields (bound to the ViewModel states)
    val name by remember { viewModel.personNameState }
    val email by remember { viewModel.personEmailState }
    val phone by remember { viewModel.personPhoneState }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        // Display the first image from the photoUris list
        if (photoUris.isNotEmpty() && uri.value != null) {
            Log.i("URI", uri.value.toString())
            AsyncImage(
                model = photoUris[0],
                contentDescription = "Captured Image",
                modifier = Modifier.size(200.dp)
            )
            Log.i("CameraX", photoUris[0].toString())
        } else {
            // Placeholder image if no image is captured yet
            Image(
                painter = painterResource(id = R.drawable.face_register),
                contentDescription = "Placeholder Image",
                modifier = Modifier.size(200.dp)
            )
        }

        // Text
        Text(
            text = "Details",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Input fields bound to ViewModel states
        OutlinedTextField(
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Person, contentDescription = "Name")
            },
            value = name, // Bound to the ViewModel's nameState
            onValueChange = { viewModel.personNameState.value = it },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        OutlinedTextField(
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Email, contentDescription = "Email")
            },
            value = email, // Bound to the ViewModel's emailState
            onValueChange = { viewModel.personEmailState.value = it },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
        OutlinedTextField(
            leadingIcon = {
                Icon(imageVector = Icons.Outlined.Phone, contentDescription = "Phone Number")
            },
            value = phone, // Bound to the ViewModel's phoneState
            onValueChange = { viewModel.personPhoneState.value = it },
            label = { Text("Phone Number") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        // Register Button
        Button(
            onClick = {
                // Logic to handle the registration or further processing

            },
            colors = ButtonDefaults.buttonColors(containerColor = claret), // Example color
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
    }
}


@Composable
private fun ImageReadProgressDialog(viewModel: AddFaceScreenViewModel, onNavigateBack: () -> Unit) {
    val isProcessing by remember { viewModel.isProcessingImages }
    val numImagesProcessed by remember { viewModel.numImagesProcessed }
    val context = LocalContext.current
    AppProgressDialog()
    if (isProcessing) {
        showProgressDialog()
    } else {
        if (numImagesProcessed > 0) {
            onNavigateBack()
            Toast.makeText(context, "Added to database", Toast.LENGTH_SHORT).show()
        }
        hideProgressDialog()
    }
}

@Preview
@Composable
fun RegisterScreenPreview() {
    RegisterScreen() {}
}

