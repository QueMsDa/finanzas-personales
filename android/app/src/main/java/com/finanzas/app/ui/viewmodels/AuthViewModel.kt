package com.finanzas.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.app.data.SupabaseConfig
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val nombreUsuario: String = ""
)

class AuthViewModel : ViewModel() {

    private val auth = SupabaseConfig.client.auth

    val sessionStatus: StateFlow<SessionStatus> = auth.sessionStatus
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionStatus.NotAuthenticated(false))

    private val _state = MutableStateFlow(AuthState())
    val state = _state.asStateFlow()

    val isLoggedIn: Boolean
        get() = auth.currentUserOrNull() != null

    val nombreActual: String
        get() = auth.currentUserOrNull()
            ?.userMetadata?.get("nombre")?.toString()
            ?.trim('"') ?: ""

    // ── Login ──────────────────────────────────────────────────
    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Completa todos los campos") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                auth.signInWith(Email) {
                    this.email    = email.trim()
                    this.password = password
                }
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "Correo o contraseña incorrectos") }
            }
        }
    }

    // ── Registro ───────────────────────────────────────────────
    fun register(nombre: String, email: String, password: String) {
        if (nombre.isBlank() || email.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "Completa todos los campos") }
            return
        }
        if (password.length < 6) {
            _state.update { it.copy(error = "La contraseña debe tener al menos 6 caracteres") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                auth.signUpWith(Email) {
                    this.email    = email.trim()
                    this.password = password
                    data = buildJsonObject { put("nombre", nombre.trim()) }
                }
                _state.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = "No se pudo registrar: ${e.message}") }
            }
        }
    }

    // ── Cerrar sesión ──────────────────────────────────────────
    fun logout() {
        viewModelScope.launch {
            try { auth.signOut() } catch (_: Exception) {}
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
