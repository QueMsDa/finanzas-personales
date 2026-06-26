package com.finanzas.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.finanzas.app.ui.viewmodels.GastoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YapeScreen(
    vm: GastoViewModel = viewModel(),
    onSaved: () -> Unit = {}
) {
    val state   by vm.yapeState.collectAsState()
    val context = LocalContext.current
    var dropdownExpanded by remember { mutableStateOf(false) }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { vm.setImageUri(it, context) }
    }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            vm.resetSaved()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Importar Yape") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = Color(0xFF6B27E8),
                    titleContentColor = Color.White
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

            // ── Selector de imagen ─────────────────────────────────────────
            if (state.imageUri == null) {
                OutlinedButton(
                    onClick   = { pickImage.launch("image/*") },
                    modifier  = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape     = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF6B27E8)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Seleccionar captura de Yape",
                            color = Color(0xFF6B27E8),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Toca aquí para elegir una imagen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Vista previa de la imagen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(2.dp, Color(0xFF6B27E8), RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model              = state.imageUri,
                        contentDescription = "Captura de Yape",
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.fillMaxSize()
                    )
                }
                TextButton(
                    onClick = { pickImage.launch("image/*") },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cambiar imagen")
                }
            }

            // ── Procesando ─────────────────────────────────────────────────
            if (state.isProcessing) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color    = Color(0xFF6B27E8),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Reconociendo monto del Yape...", color = Color(0xFF6B27E8))
                    }
                }
            }

            // ── Resultado OCR ──────────────────────────────────────────────
            if (!state.isProcessing && state.imageUri != null) {
                Text(
                    "Datos detectados (puedes editar)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value         = state.montoDetectado,
                    onValueChange = vm::setYapeMonto,
                    label         = { Text("Monto detectado (S/)") },
                    prefix        = { Text("S/ ") },
                    modifier      = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine    = true,
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Color(0xFF6B27E8),
                        focusedLabelColor    = Color(0xFF6B27E8)
                    )
                )

                OutlinedTextField(
                    value         = state.destinatario,
                    onValueChange = vm::setYapeDestinatario,
                    label         = { Text("Destinatario (opcional)") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )

                // Categoría
                ExposedDropdownMenuBox(
                    expanded         = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = state.categorias.find { it.id == state.categoriaId }?.nombre ?: "Seleccionar",
                        onValueChange = {},
                        readOnly  = true,
                        label     = { Text("Categoría") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
                        modifier  = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded         = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        state.categorias.forEach { cat ->
                            DropdownMenuItem(
                                text    = { Text(cat.nombre) },
                                onClick = {
                                    vm.setYapeCategoria(cat.id)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Error ──────────────────────────────────────────────────────
            state.error?.let {
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            // ── Guardar ────────────────────────────────────────────────────
            if (!state.isProcessing && state.imageUri != null) {
                Button(
                    onClick   = vm::guardarGastoYape,
                    enabled   = !state.isSaving && state.montoDetectado.isNotBlank(),
                    modifier  = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors    = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6B27E8)
                    )
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            color       = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("📱 Guardar gasto de Yape", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
