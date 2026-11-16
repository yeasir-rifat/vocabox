package com.vocabulary.app.ui.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vocabulary.app.data.auth.AuthRepository
import com.vocabulary.app.data.auth.AuthState
import com.vocabulary.app.data.remote.RemoteWordSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives the login gate. Whenever someone is signed in, the word list is synced from Firebase. */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val sync: RemoteWordSync
) : ViewModel() {

    val authState: StateFlow<AuthState> = auth.state
        .stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val isConfigured: Boolean get() = auth.isConfigured

    init {
        viewModelScope.launch {
            auth.state
                .filterIsInstance<AuthState.SignedIn>()
                .map { it.uid }
                .distinctUntilChanged()
                .collect { launch { sync.sync() } }
        }
    }

    fun signIn(activityContext: Context) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _error.value = null
            auth.signInWithGoogle(activityContext).onFailure { _error.value = friendly(it) }
            _busy.value = false
        }
    }

    private fun friendly(e: Throwable): String? = when (e) {
        is GetCredentialCancellationException -> null // the person closed the picker
        is NoCredentialException -> "No Google account found on this phone. Add one in the phone settings, then try again."
        is AuthRepository.SetupException -> e.message
        else -> "Sign-in failed. Check your internet connection and try again."
    }
}
