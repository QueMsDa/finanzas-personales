package com.finanzas.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzas.app.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(authVm: AuthViewModel = viewModel()) {
    val state by authVm.state.collectAsState()

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nombre   by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // ── Logo / Título ──────────────────────────────────────
        Text("💰", fontSize = 64.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Finanzas Personal",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = if (isRegisterMode) "Crea tu cuenta" else "Inicia sesión para continuar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(32.dp))

        // ── Nombre (solo en registro) ──────────────────────────
        if (isRegisterMode) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Tu nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(12.dp))
        }

        // ── Email ──────────────────────────────────────────────
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; authVm.clearError() },
            label = { Text("Correo electrónico") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        // ── Contraseña ─────────────────────────────────────────
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; authVm.clearError() },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { showPass = !showPass }) {
                    Icon(
                        imageVector = if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            }
        )

        // ── Error ──────────────────────────────────────────────
        state.error?.let {
            Spacer(Modifier.height(8.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))

        // ── Botón principal ────────────────────────────────────
        Button(
            onClick = {
                if (isRegisterMode) authVm.register(nombre, email, password)
                else authVm.login(email, password)
            },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isRegisterMode) "Crear cuenta" else "Entrar",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Cambiar modo ───────────────────────────────────────
        TextButton(onClick = {
            isRegisterMode = !isRegisterMode
            authVm.clearError()
        }) {
            Text(
                text = if (isRegisterMode)
                    "¿Ya tienes cuenta? Inicia sesión"
                else
                    "¿No tienes cuenta? Regístrate"
            )
        }

        Spacer(Modifier.height(24.dp))

        // ── Tip para la pareja ─────────────────────────────────
        if (!isRegisterMode) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "💡 Tu pareja puede crear su propia cuenta y ver los gastos compartidos",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
