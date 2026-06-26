package com.finanzas.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.app.data.models.Categoria
import com.finanzas.app.data.models.GastoConCategoria
import com.finanzas.app.data.repository.GastoRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val gastos: List<GastoConCategoria> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val totalMes: Double get() = gastos.sumOf { it.monto }

    val porCategoria: List<Pair<String, Double>> get() =
        gastos
            .groupBy { it.nombreCategoria }
            .map { (cat, list) -> cat to list.sumOf { it.monto } }
            .sortedByDescending { it.second }

    val recientes: List<GastoConCategoria> get() = gastos.take(5)
}

class DashboardViewModel : ViewModel() {

    private val repo = GastoRepository()

    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()

    init {
        cargarDatos()
        suscribirRealtime()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val gastos     = repo.getGastosMesActual()
                val categorias = repo.getCategorias()
                _state.update { it.copy(gastos = gastos, categorias = categorias, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun suscribirRealtime() {
        viewModelScope.launch {
            try {
                repo.observarCambios().collect { cargarDatos() }
            } catch (_: Exception) {
                // Realtime no disponible — el usuario puede hacer pull-to-refresh
            }
        }
    }
}
