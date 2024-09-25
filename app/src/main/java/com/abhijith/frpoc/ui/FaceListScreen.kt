package com.abhijith.frpoc.ui

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.abhijith.frpoc.components.AppAlertDialog
import com.abhijith.frpoc.components.createAlertDialog
import com.abhijith.frpoc.database.ImagesVectorDB
import com.abhijith.frpoc.database.PersonDB
import com.abhijith.frpoc.database.PersonRecord
import com.abhijith.frpoc.helper.FaceNet
import com.abhijith.frpoc.helper.ImageVectorUseCase
import com.abhijith.frpoc.helper.MediaPipeFaceDetector
import com.abhijith.frpoc.helper.PersonUseCase
import com.abhijith.frpoc.ui.theme.FRPOCTheme
import com.abhijith.frpoc.ui.theme.claret
import com.abhijith.frpoc.viewmodel.FaceListScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceListScreen(context: Context, onNavigateBack: (() -> Unit), onAddFaceClick: (() -> Unit)) {
    FRPOCTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = "Face List", style = MaterialTheme.typography.headlineSmall)
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = claret
                    ),
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = "Navigate Back"
                            )
                        }
                    },
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAddFaceClick, containerColor = claret) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add a new face")
                }
            }
        ) { innerPadding ->
            val personDB = PersonDB()
            val imagesVectorDB = ImagesVectorDB()
            val faceNet = FaceNet(context)
            val mediaPipeFaceDetector = MediaPipeFaceDetector(context)
            val imageVectorUseCase = ImageVectorUseCase(
                mediapipeFaceDetector = mediaPipeFaceDetector,
                imagesVectorDB = imagesVectorDB,
                faceNet = faceNet
            )
            val viewModel =
                remember { FaceListScreenViewModel(imageVectorUseCase, PersonUseCase(personDB)) }
            Column(modifier = Modifier.padding(innerPadding)) {
                ScreenUI(viewModel)
                AppAlertDialog()
            }
        }
    }
}

@Composable
private fun ScreenUI(viewModel: FaceListScreenViewModel) {
    val faces by viewModel.personFlow.collectAsState(emptyList())
    LazyColumn { items(faces) { FaceListItem(it) { viewModel.removeFace(it.personID) } } }
}

@Composable
private fun FaceListItem(personRecord: PersonRecord, onRemoveFaceClick: (() -> Unit)) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Text(
                text = personRecord.personName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = DateUtils.getRelativeTimeSpanString(personRecord.addTime).toString(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.DarkGray
            )
        }
        Icon(
            modifier =
            Modifier.clickable {
                createAlertDialog(
                    dialogTitle = "Remove person",
                    dialogText =
                    "Are you sure to remove this person from the database? The face for this person will no longer " +
                            "be detected in real-time.",
                    dialogPositiveButtonText = "Remove",
                    onPositiveButtonClick = onRemoveFaceClick,
                    dialogNegativeButtonText = "Cancel",
                    onNegativeButtonClick = {}
                )
            },
            imageVector = Icons.Default.Clear,
            contentDescription = "Remove face"
        )
        Spacer(modifier = Modifier.width(2.dp))
    }
}
