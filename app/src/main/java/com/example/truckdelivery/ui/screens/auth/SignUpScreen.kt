package com.example.truckdelivery.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.truckdelivery.data.model.Resource
import com.example.truckdelivery.data.model.UserType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateBack: () -> Unit,
    onSignUpSuccess: (String) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedUserType by remember { mutableStateOf(UserType.SALES_POINT) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.signUpState) {
        when (val state = viewModel.signUpState) {
            is Resource.Success -> {
                onSignUpSuccess(state.data.userType.toString())
            }
            is Resource.Error -> {
                showError = true
                errorMessage = state.exception.message ?: "An error occurred"
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign Up") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        // Use a back arrow icon
                        Text("←")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = password != confirmPassword && confirmPassword.isNotEmpty()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Type Selection
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Select User Type",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    UserType.values().forEach { userType ->
                        FilterChip(
                            selected = selectedUserType == userType,
                            onClick = { selectedUserType = userType },
                            label = { 
                                Text(
                                    userType.toString()
                                        .replace("_", " ")
                                        .lowercase()
                                        .capitalize()
                                ) 
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error message
            if (showError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up button
            Button(
                onClick = {
                    if (password != confirmPassword) {
                        showError = true
                        errorMessage = "Passwords do not match"
                        return@Button
                    }
                    showError = false
                    viewModel.signUp(email, password, selectedUserType)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = email.isNotBlank() && 
                         password.isNotBlank() && 
                         confirmPassword.isNotBlank() &&
                         password == confirmPassword
            ) {
                if (viewModel.signUpState is Resource.Loading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Sign Up")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Login link
            TextButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Already have an account? Login")
            }
        }
    }
}
