package com.finanzas.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzas.app.ui.viewmodels.GastoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarGastoScreen(
    vm: GastoViewModel = viewModel(),
    onSaved: () -> Unit = {}
) {
    val state by vm.agregarState.collectAsState()
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            vm.resetSaved()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar Gasto") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Monto ──────────────────────────────────────────────────────
            OutlinedTextField(
                value         = state.monto,
                onValueChange = vm::setMonto,
                label         = { Text("Monto (S/)") },
                prefix        = { Text("S/ ") },
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                isError       = state.error != null && state.monto.isBlank()
            )

            // ── Categoría ──────────────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded        = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = state.categorias.find { it.id == state.categoriaId }?.nombre ?: "Seleccionar",
                    onValueChange = {},
                    readOnly  = true,
                    label     = { Text("Categoría") },
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded        = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    state.categorias.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.nombre) },
                            onClick = {
                                vm.setCategoria(cat.id)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // ── Tipo: Personal / Compartido ───────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("personal" to "🔵 Personal", "compartido" to "💚 Compartido").forEach { (valor, label) ->
                    FilterChip(
                        selected = state.tipo == valor,
                        onClick  = { vm.setTipo(valor) },
                        label    = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Descripción ────────────────────────────────────────────────
            OutlinedTextField(
                value         = state.descripcion,
                onValueChange = vm::setDescripcion,
                label         = { Text("Descripción (opcional)") },
                modifier      = Modifier.fillMaxWidth(),
                maxLines      = 3
            )

            // ── Error ──────────────────────────────────────────────────────
            state.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            // ── Guardar ────────────────────────────────────────────────────
            Button(
                onClick   = vm::guardarGasto,
                enabled   = !state.isSaving,
                modifier  = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color    = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Guardar Gasto", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
