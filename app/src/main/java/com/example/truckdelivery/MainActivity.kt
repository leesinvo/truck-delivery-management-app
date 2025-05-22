package com.example.truckdelivery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.truckdelivery.ui.theme.TruckDeliveryTheme
import com.example.truckdelivery.ui.screens.auth.LoginScreen
import com.example.truckdelivery.ui.screens.auth.SignUpScreen
import com.example.truckdelivery.ui.screens.driver.DriverDashboardScreen
import com.example.truckdelivery.ui.screens.salespoint.SalesPointDashboardScreen
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TruckDeliveryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TruckDeliveryApp()
                }
            }
        }
    }
}

@Composable
fun TruckDeliveryApp() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
            "home"
        } else {
            "login"
        }
    ) {
        // Auth screens
        composable("login") {
            LoginScreen(
                onNavigateToSignUp = { navController.navigate("signup") },
                onLoginSuccess = { userType ->
                    navController.navigate(
                        when (userType) {
                            "TRUCK_DRIVER" -> "driver_dashboard"
                            else -> "salespoint_dashboard"
                        }
                    ) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        
        composable("signup") {
            SignUpScreen(
                onNavigateBack = { navController.navigateUp() },
                onSignUpSuccess = { userType ->
                    navController.navigate(
                        when (userType) {
                            "TRUCK_DRIVER" -> "driver_dashboard"
                            else -> "salespoint_dashboard"
                        }
                    ) {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // Driver screens
        composable("driver_dashboard") {
            DriverDashboardScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Sales Point screens
        composable("salespoint_dashboard") {
            SalesPointDashboardScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
