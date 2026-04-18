package com.example.myapplication

import android.app.Application
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.AiExpenseParser
import com.example.myapplication.data.ThemePreferences
import com.example.myapplication.data.dataStore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(val user: FirebaseUser) : AuthState()
    data class EmailNotVerified(val user: FirebaseUser) : AuthState()
    data object Guest : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val IS_GUEST_MODE_KEY = booleanPreferencesKey("is_guest_mode")
    }

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var isGuestMode = false
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _resetEmailState = MutableStateFlow<String?>(null)
    val resetEmailState: StateFlow<String?> = _resetEmailState.asStateFlow()

    private val _userPlan = MutableStateFlow("free")
    val userPlan: StateFlow<String> = _userPlan.asStateFlow()

    private val _planExpiresAt = MutableStateFlow<String?>(null)
    val planExpiresAt: StateFlow<String?> = _planExpiresAt.asStateFlow()

    private val _displayNameUpdateStatus = MutableStateFlow("")
    val displayNameUpdateStatus: StateFlow<String> = _displayNameUpdateStatus.asStateFlow()

    private fun toAuthState(user: FirebaseUser?): AuthState {
        return when {
            user == null && isGuestMode -> AuthState.Guest
            user == null -> AuthState.Unauthenticated
            user.isEmailVerified -> AuthState.Authenticated(user)
            else -> AuthState.EmailNotVerified(user)
        }
    }

    private val authStateListener = AuthStateListener { firebaseAuth ->
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            _authState.value = toAuthState(currentUser)
            refreshSubscriptionStatus()
        } else {
            viewModelScope.launch {
                isGuestMode = readGuestModeFlag()
                _authState.value = toAuthState(null)
            }
        }
    }

    init {
        viewModelScope.launch {
            isGuestMode = readGuestModeFlag()
        }
        auth.addAuthStateListener(authStateListener)
    }

    private suspend fun readGuestModeFlag(): Boolean {
        return getApplication<Application>().dataStore.data.first()[IS_GUEST_MODE_KEY] ?: false
    }

    fun refreshSubscriptionStatus() {
        viewModelScope.launch {
            val result = AiExpenseParser.fetchSubscriptionStatus()
            result.onSuccess { status ->
                _userPlan.value = status.plan
                _planExpiresAt.value = status.expiresAt
                val prefs = ThemePreferences(getApplication())
                prefs.setUserPlan(status.plan, status.expiresAt)
            }
        }
    }

    suspend fun updateDisplayName(newName: String): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(Exception("No authenticated user"))

        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) {
            val error = Exception("Display name cannot be empty")
            _displayNameUpdateStatus.value = error.message ?: "Update failed"
            return Result.failure(error)
        }

        return try {
            _displayNameUpdateStatus.value = ""
            suspendCancellableCoroutine { continuation ->
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(trimmedName)
                    .build()

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            _displayNameUpdateStatus.value = "success"
                            _authState.value = toAuthState(auth.currentUser)
                            continuation.resume(Result.success(Unit))
                        } else {
                            val exception = task.exception ?: Exception("Failed to update display name")
                            _displayNameUpdateStatus.value = exception.message ?: "Failed to update display name"
                            continuation.resume(Result.failure(exception))
                        }
                    }
            }
        } catch (e: Exception) {
            _displayNameUpdateStatus.value = e.message ?: "Failed to update display name"
            Result.failure(e)
        }
    }

    fun clearDisplayNameUpdateStatus() {
        _displayNameUpdateStatus.value = ""
    }

    private suspend fun setGuestModeFlag(enabled: Boolean) {
        isGuestMode = enabled
        getApplication<Application>().dataStore.edit { preferences ->
            preferences[IS_GUEST_MODE_KEY] = enabled
        }
    }

    fun signInWithEmail(email: String, password: String) {
        isGuestMode = false
        _authState.value = AuthState.Loading
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    viewModelScope.launch {
                        setGuestModeFlag(false)
                        _authState.value = toAuthState(auth.currentUser)
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Email sign-in failed")
                }
            }
    }

    fun signUpWithEmail(email: String, password: String) {
        isGuestMode = false
        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    sendEmailVerification()
                    viewModelScope.launch {
                        setGuestModeFlag(false)
                        _authState.value = toAuthState(auth.currentUser)
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Email sign-up failed")
                }
            }
    }

    fun signInWithGoogle(idToken: String) {
        isGuestMode = false
        _authState.value = AuthState.Loading
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    viewModelScope.launch {
                        setGuestModeFlag(false)
                        _authState.value = toAuthState(auth.currentUser)
                    }
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
        isGuestMode = false
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    fun enterGuestMode() {
        viewModelScope.launch {
            setGuestModeFlag(true)
            _authState.value = AuthState.Guest
        }
    }

    fun signOutWithClearData(clearLocal: Boolean) {
        viewModelScope.launch {
            if (clearLocal) {
                // Placeholder: local data clearing will be implemented later.
            }
            setGuestModeFlag(false)
            auth.signOut()
            _authState.value = AuthState.Unauthenticated
        }
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
