package com.finanzas.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzas.app.ui.components.GastoItem
import com.finanzas.app.ui.viewmodels.GastoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(vm: GastoViewModel = viewModel()) {
    val state by vm.historialState.collectAsState()

    LaunchedEffect(Unit) { vm.cargarHistorial() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Gastos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh    = { vm.cargarHistorial() },
            modifier     = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Buscador ───────────────────────────────────────────────
                OutlinedTextField(
                    value         = state.busqueda,
                    onValueChange = vm::setBusqueda,
                    label         = { Text("Buscar...") },
                    leadingIcon   = { Icon(Icons.Default.Search, null) },
                    trailingIcon  = {
                        if (state.busqueda.isNotBlank()) {
                            IconButton(onClick = { vm.setBusqueda("") }) {
                                Icon(Icons.Default.Clear, "Limpiar")
                            }
                        }
                    },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine    = true
                )

                // ── Filtros por categoría ──────────────────────────────────
                val categorias = state.gastos.map { it.nombreCategoria }.distinct()
                if (categorias.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = state.filtroCategoria == null,
                                onClick  = { vm.setFiltroCategoria(null) },
                                label    = { Text("Todos") }
                            )
                        }
                        items(state.gastos.map { it.categorias }.distinctBy { it?.id }) { cat ->
                            cat ?: return@items
                            FilterChip(
                                selected = state.filtroCategoria == cat.id,
                                onClick  = {
                                    vm.setFiltroCategoria(
                                        if (state.filtroCategoria == cat.id) null else cat.id
                                    )
                                },
                                label    = { Text(cat.nombre) }
                            )
                        }
                    }
                }

                // ── Total filtrado ─────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text(
                        "${state.gastosFiltrados.size} gastos",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "S/ %.2f".format(state.totalFiltrado),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))

                // ── Lista ──────────────────────────────────────────────────
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.gastosFiltrados, key = { it.id }) { gasto ->
                        GastoItem(
                            gasto    = gasto,
                            onDelete = { vm.eliminarGasto(gasto.id) }
                        )
                    }

                    if (!state.isLoading && state.gastosFiltrados.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("📋", style = MaterialTheme.typography.displayMedium)
                                Text("Sin gastos registrados")
                            }
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}
