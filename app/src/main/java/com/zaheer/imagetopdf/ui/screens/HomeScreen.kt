package com.zaheer.imagetopdf.ui.screens

import android.app.Activity
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
 * Home screen for selecting images and initiating conversion
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ImageToPdfViewModel,
    onNavigateToPreview: () -> Unit
) {
    val context = LocalContext.current
    val selectedImages by viewModel.selectedImages.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val message by viewModel.message.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val isPro by viewModel.billingManager.isPro.collectAsState()
    
    var showProDialog by remember { mutableStateOf(false) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.addImages(uris)
        }
    }
    
    // Handle messages
    LaunchedEffect(message) {
        message?.let {
            viewModel.clearMessage()
        }
    }
    
    LaunchedEffect(successMessage) {
        successMessage?.let {
            viewModel.clearSuccessMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    if (!isPro) {
                        IconButton(onClick = { showProDialog = true }) {
                            Icon(Icons.Default.Star, contentDescription = "Upgrade to Pro")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedImages.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.convertToPdf()
                        onNavigateToPreview()
                    },
                    icon = { Icon(Icons.Default.Send, contentDescription = null) },
                    text = { Text(stringResource(R.string.convert_to_pdf)) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pro badge
            if (isPro) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "PRO VERSION",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            // Select images button
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isProcessing
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.select_images))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Selected images info
            if (selectedImages.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_images_selected),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.images_selected, selectedImages.size),
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (!isPro && selectedImages.size >= 5) {
                    Text(
                        text = "Free limit reached",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // List of selected images
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(selectedImages) { index, uri ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Create, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Image ${index + 1}")
                                }
                                
                                Row {
                                    if (index > 0) {
                                        IconButton(onClick = { 
                                            viewModel.moveImage(index, index - 1) 
                                        }) {
                                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                                        }
                                    }
                                    if (index < selectedImages.size - 1) {
                                        IconButton(onClick = { 
                                            viewModel.moveImage(index, index + 1) 
                                        }) {
                                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                                        }
                                    }
                                    IconButton(onClick = { viewModel.removeImage(index) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Loading indicator
            if (isProcessing) {
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
        
        // Show messages
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
    
    // Pro upgrade dialog
    if (showProDialog) {
        ProUpgradeDialog(
            viewModel = viewModel,
            onDismiss = { showProDialog = false }
        )
    }
}

/**
 * Dialog for upgrading to Pro
 */
@Composable
fun ProUpgradeDialog(
    viewModel: ImageToPdfViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val productPrice = remember { viewModel.billingManager.getProductPrice() ?: "..." }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Star, contentDescription = null) },
        title = { Text(stringResource(R.string.upgrade_to_pro)) },
        text = {
            Column {
                Text(stringResource(R.string.pro_features_title))
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pro_feature_unlimited))
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.pro_feature_no_watermark))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Price: $productPrice", style = MaterialTheme.typography.titleMedium)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    (context as? Activity)?.let { activity ->
                        kotlinx.coroutines.MainScope().launch {
                            viewModel.billingManager.launchPurchaseFlow(activity)
                        }
                    }
                    onDismiss()
                }
            ) {
                Text("Purchase")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    viewModel.restorePurchases()
                    onDismiss()
                }) {
                    Text(stringResource(R.string.restore_purchases))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}
