package com.example.truckdelivery.ui.screens.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.truckdelivery.data.model.Resource
import com.example.truckdelivery.data.model.User
import com.example.truckdelivery.data.model.UserType
import com.example.truckdelivery.data.repository.FirebaseRepository
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    var loginState: Resource<User> by mutableStateOf(Resource.Success(User()))
        private set

    var signUpState: Resource<User> by mutableStateOf(Resource.Success(User()))
        private set

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                loginState = Resource.Loading
                val result = repository.signIn(email, password)
                result.fold(
                    onSuccess = { firebaseUser ->
                        // Fetch user type from Firestore
                        repository.getUserType(firebaseUser.uid)?.let { userType ->
                            loginState = Resource.Success(
                                User(
                                    id = firebaseUser.uid,
                                    email = firebaseUser.email ?: "",
                                    userType = userType
                                )
                            )
                        } ?: run {
                            loginState = Resource.Error(Exception("User type not found"))
                        }
                    },
                    onFailure = { exception ->
                        loginState = Resource.Error(exception)
                    }
                )
            } catch (e: Exception) {
                loginState = Resource.Error(e)
            }
        }
    }

    fun signUp(email: String, password: String, userType: UserType) {
        viewModelScope.launch {
            try {
                signUpState = Resource.Loading
                val result = repository.signUp(email, password, userType.toString())
                result.fold(
                    onSuccess = { firebaseUser ->
                        signUpState = Resource.Success(
                            User(
                                id = firebaseUser.uid,
                                email = firebaseUser.email ?: "",
                                userType = userType
                            )
                        )
                    },
                    onFailure = { exception ->
                        signUpState = Resource.Error(exception)
                    }
                )
            } catch (e: Exception) {
                signUpState = Resource.Error(e)
            }
        }
    }

    fun resetStates() {
        loginState = Resource.Success(User())
        signUpState = Resource.Success(User())
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
