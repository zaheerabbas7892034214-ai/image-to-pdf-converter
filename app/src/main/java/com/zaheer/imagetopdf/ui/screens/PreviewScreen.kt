package com.zaheer.imagetopdf.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zaheer.imagetopdf.R
import com.zaheer.imagetopdf.ui.viewmodel.ImageToPdfViewModel

/**
 * Preview screen showing the generated PDF and options to save/share
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewScreen(
    viewModel: ImageToPdfViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val selectedImages by viewModel.selectedImages.collectAsState()
    val generatedPdfFile by viewModel.generatedPdfFile.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val message by viewModel.message.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isPro by viewModel.billingManager.isPro.collectAsState()
    
    // Auto-convert if not already done
    LaunchedEffect(Unit) {
        if (generatedPdfFile == null && selectedImages.isNotEmpty()) {
            viewModel.convertToPdf()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preview_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isProcessing -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.loading),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                generatedPdfFile != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Success icon
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "PDF Ready!",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "${selectedImages.size} ${if (selectedImages.size == 1) "image" else "images"} converted",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (!isPro) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ Free version includes watermark",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        // Save button
                        Button(
                            onClick = { viewModel.savePdf() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Default.Done, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.save_pdf))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Share button
                        OutlinedButton(
                            onClick = {
                                viewModel.sharePdf()?.let { intent ->
                                    context.startActivity(Intent.createChooser(intent, "Share PDF"))
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.share_pdf))
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Create new button
                        TextButton(
                            onClick = {
                                viewModel.clearImages()
                                onNavigateBack()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create New PDF")
                        }
                    }
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.error_no_images),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
        
        // Show messages as snackbars
        message?.let {
            Snackbar(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(it)
            }
        }
        
        successMessage?.let {
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(it)
            }
        }
    }
}
