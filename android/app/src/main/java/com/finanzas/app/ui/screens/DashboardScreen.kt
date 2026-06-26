package com.finanzas.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.finanzas.app.ui.components.BarChart
import com.finanzas.app.ui.components.GastoItem
import com.finanzas.app.ui.viewmodels.DashboardViewModel
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: DashboardViewModel = viewModel()) {
    val state by vm.state.collectAsState()

    val mesNombre = LocalDate.now()
        .month.getDisplayName(TextStyle.FULL, Locale("es", "PE"))
        .replaceFirstChar { it.uppercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finanzas Personal") },
                actions = {
                    IconButton(onClick = { vm.cargarDatos() }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh    = { vm.cargarDatos() },
            modifier     = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {

                // ── Tarjeta total del mes ──────────────────────────────────
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total gastado en $mesNombre",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "S/ %.2f".format(state.totalMes),
                                fontSize   = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${state.gastos.size} transacciones",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // ── Gráfico por categoría ──────────────────────────────────
                if (state.porCategoria.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "Gasto por categoría",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(12.dp))
                                BarChart(datos = state.porCategoria)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                // ── Gastos recientes ───────────────────────────────────────
                if (state.recientes.isNotEmpty()) {
                    item {
                        Text(
                            text = "Últimos gastos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(state.recientes) { gasto ->
                        GastoItem(
                            gasto    = gasto,
                            onDelete = {}  // Solo lectura en el dashboard
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }

                // ── Estado vacío ───────────────────────────────────────────
                if (!state.isLoading && state.gastos.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("💰", fontSize = 48.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Aún no tienes gastos este mes",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Agrega tu primer gasto con el botón +",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Error ──────────────────────────────────────────────────
                state.error?.let { err ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = "Error: $err",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}
