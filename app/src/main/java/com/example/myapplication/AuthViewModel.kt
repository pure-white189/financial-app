package com.example.myapplication

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class EmailNotVerified(val user: FirebaseUser) : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _resetEmailState = MutableStateFlow<String?>(null)
    val resetEmailState: StateFlow<String?> = _resetEmailState.asStateFlow()

    private fun toAuthState(user: FirebaseUser?): AuthState {
        return when {
            user == null -> AuthState.Unauthenticated
            user.isEmailVerified -> AuthState.Authenticated(user)
            else -> AuthState.EmailNotVerified(user)
        }
    }

    private val authStateListener = AuthStateListener { firebaseAuth ->
        val currentUser = firebaseAuth.currentUser
        _authState.value = toAuthState(currentUser)
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    fun signInWithEmail(email: String, password: String) {
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = toAuthState(auth.currentUser)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Email sign-in failed")
                }
            }
    }

    fun signUpWithEmail(email: String, password: String) {
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sendEmailVerification()
                    _authState.value = toAuthState(auth.currentUser)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Email sign-up failed")
                }
            }
    }

    fun signInWithGoogle(idToken: String) {
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = toAuthState(auth.currentUser)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Google sign-in failed")
                }
            }
    }

    fun sendEmailVerification() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        user.sendEmailVerification()
    }

    fun reloadUser() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        _authState.value = AuthState.Loading
        user.reload()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = toAuthState(auth.currentUser)
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Failed to refresh user")
                }
            }
    }

    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun sendPasswordResetEmail(email: String) {
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _resetEmailState.value = "sent"
                } else {
                    val message = task.exception?.message ?: "Failed to send password reset email"
                    _resetEmailState.value = "error: $message"
                }
            }
    }

    fun clearResetEmailState() {
        _resetEmailState.value = null
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        super.onCleared()
    }
}

