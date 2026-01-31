package com.zaheer.imagetopdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.zaheer.imagetopdf.ui.screens.HomeScreen
import com.zaheer.imagetopdf.ui.screens.PreviewScreen
import com.zaheer.imagetopdf.ui.theme.ImageToPDFConverterTheme
import com.zaheer.imagetopdf.ui.viewmodel.ImageToPdfViewModel

/**
 * Main Activity - Single Activity architecture with Jetpack Compose
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageToPDFConverterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImageToPdfApp()
                }
            }
        }
    }
}

/**
 * Main app composable with navigation
 */
@Composable
fun ImageToPdfApp() {
    val navController = rememberNavController()
    val viewModel: ImageToPdfViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToPreview = {
                    navController.navigate("preview")
                }
            )
        }
        
        composable("preview") {
            PreviewScreen(
                viewModel = viewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
