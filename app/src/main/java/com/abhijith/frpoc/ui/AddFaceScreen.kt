package com.abhijith.frpoc.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhijith.frpoc.components.AppProgressDialog
import com.abhijith.frpoc.components.DelayedVisibility
import com.abhijith.frpoc.components.hideProgressDialog
import com.abhijith.frpoc.components.showProgressDialog
import com.abhijith.frpoc.viewmodel.AddFaceScreenViewModel
import coil.compose.AsyncImage
import com.abhijith.frpoc.ui.theme.FRPOCTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFaceScreen(viewModel: AddFaceScreenViewModel,onNavigateBack: () -> Unit) {
    FRPOCTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text(text = "Add Faces", style = MaterialTheme.typography.headlineSmall) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Navigate Back"
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
    val pickVisualMediaLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickMultipleVisualMedia()
        ) {
            viewModel.selectedImageURIs.value = it
        }
    var personName by remember { viewModel.personNameState }
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = personName,
            onValueChange = { personName = it },
            label = { Text(text = "Enter the person's name") },
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(
                enabled = viewModel.personNameState.value.isNotEmpty(),
                onClick = {
                    pickVisualMediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = "Choose photos")
                Text(text = "Choose photos")
            }
            DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
                Button(onClick = { viewModel.addImages() }) { Text(text = "Add to database") }
            }
        }
        DelayedVisibility(viewModel.selectedImageURIs.value.isNotEmpty()) {
            Text(
                text = "${viewModel.selectedImageURIs.value.size} image(s) selected",
                style = MaterialTheme.typography.labelSmall
            )
        }
        ImagesGrid(viewModel)
    }
}

@Composable
private fun ImagesGrid(viewModel: AddFaceScreenViewModel) {
    val uris by remember { viewModel.selectedImageURIs }
    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(uris) { uri ->
            AsyncImage(model = uri, contentDescription = null)
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
