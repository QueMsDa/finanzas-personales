package com.finanzas.app.ui.viewmodels

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.finanzas.app.data.models.Categoria
import com.finanzas.app.data.models.Gasto
import com.finanzas.app.data.models.GastoConCategoria
import com.finanzas.app.data.repository.GastoRepository
import com.finanzas.app.ocr.YapeRecognizer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// ─── Estado del formulario de agregar gasto ───────────────────────────────────
data class AgregarState(
    val monto: String = "",
    val categoriaId: Long = 0,
    val descripcion: String = "",
    val tipo: String = "personal",
    val categorias: List<Categoria> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

// ─── Estado del historial ─────────────────────────────────────────────────────
data class HistorialState(
    val gastos: List<GastoConCategoria> = emptyList(),
    val isLoading: Boolean = false,
    val filtroCategoria: Long? = null,
    val busqueda: String = "",
    val error: String? = null
) {
    val gastosFiltrados: List<GastoConCategoria> get() = gastos.filter { g ->
        (filtroCategoria == null || g.categoriaId == filtroCategoria) &&
        (busqueda.isBlank() || g.descripcion?.contains(busqueda, ignoreCase = true) == true
                || g.nombreCategoria.contains(busqueda, ignoreCase = true)
                || g.yapeDestinatario?.contains(busqueda, ignoreCase = true) == true)
    }

    val totalFiltrado: Double get() = gastosFiltrados.sumOf { it.monto }
}

// ─── Estado del escáner Yape ──────────────────────────────────────────────────
data class YapeState(
    val imageUri: Uri? = null,
    val montoDetectado: String = "",
    val destinatario: String = "",
    val categoriaId: Long = 0,
    val categorias: List<Categoria> = emptyList(),
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────
class GastoViewModel : ViewModel() {

    private val repo = GastoRepository()

    // Agregar
    private val _agregarState = MutableStateFlow(AgregarState())
    val agregarState = _agregarState.asStateFlow()

    // Historial
    private val _historialState = MutableStateFlow(HistorialState())
    val historialState = _historialState.asStateFlow()

    // Yape
    private val _yapeState = MutableStateFlow(YapeState())
    val yapeState = _yapeState.asStateFlow()

    init {
        cargarCategorias()
    }

    private fun cargarCategorias() {
        viewModelScope.launch {
            try {
                val cats = repo.getCategorias()
                val primera = cats.firstOrNull()?.id ?: 0L
                _agregarState.update { it.copy(categorias = cats, categoriaId = primera) }
                _yapeState.update { it.copy(categorias = cats, categoriaId = primera) }
            } catch (_: Exception) {}
        }
    }

    // ── Formulario agregar ────────────────────────────────────────────────────
    fun setMonto(v: String)       = _agregarState.update { it.copy(monto = v) }
    fun setCategoria(id: Long)    = _agregarState.update { it.copy(categoriaId = id) }
    fun setDescripcion(v: String) = _agregarState.update { it.copy(descripcion = v) }
    fun setTipo(v: String)        = _agregarState.update { it.copy(tipo = v) }

    fun guardarGasto() {
        val s = _agregarState.value
        val monto = s.monto.replace(",", ".").toDoubleOrNull()
        if (monto == null || monto <= 0) {
            _agregarState.update { it.copy(error = "Ingresa un monto válido") }
            return
        }
        viewModelScope.launch {
            _agregarState.update { it.copy(isSaving = true, error = null) }
            try {
                repo.insertGasto(
                    Gasto(
                        monto       = monto,
                        categoriaId = s.categoriaId,
                        descripcion = s.descripcion.ifBlank { null },
                        fecha       = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        fuente      = "manual",
                        tipo        = s.tipo
                    )
                )
                _agregarState.update { AgregarState(categorias = it.categorias, categoriaId = it.categoriaId) }
                _agregarState.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _agregarState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun resetSaved() {
        _agregarState.update { it.copy(saved = false) }
        _yapeState.update { it.copy(saved = false) }
    }

    // ── Historial ─────────────────────────────────────────────────────────────
    fun cargarHistorial() {
        viewModelScope.launch {
            _historialState.update { it.copy(isLoading = true) }
            try {
                val gastos = repo.getTodosLosGastos()
                _historialState.update { it.copy(gastos = gastos, isLoading = false) }
            } catch (e: Exception) {
                _historialState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setBusqueda(v: String)       = _historialState.update { it.copy(busqueda = v) }
    fun setFiltroCategoria(id: Long?) = _historialState.update { it.copy(filtroCategoria = id) }

    fun eliminarGasto(id: Long) {
        viewModelScope.launch {
            try {
                repo.deleteGasto(id)
                _historialState.update { it.copy(gastos = it.gastos.filter { g -> g.id != id }) }
            } catch (e: Exception) {
                _historialState.update { it.copy(error = e.message) }
            }
        }
    }

    // ── Yape OCR ──────────────────────────────────────────────────────────────
    fun setImageUri(uri: Uri, context: Context) {
        _yapeState.update { it.copy(imageUri = uri, isProcessing = true, error = null) }
        viewModelScope.launch {
            try {
                val result = YapeRecognizer.reconocer(context, uri)
                _yapeState.update {
                    it.copy(
                        isProcessing    = false,
                        montoDetectado  = result.monto?.let { m -> "%.2f".format(m) } ?: "",
                        destinatario    = result.destinatario ?: ""
                    )
                }
            } catch (e: Exception) {
                _yapeState.update { it.copy(isProcessing = false, error = "No se pudo leer la imagen: ${e.message}") }
            }
        }
    }

    fun setYapeMonto(v: String)      = _yapeState.update { it.copy(montoDetectado = v) }
    fun setYapeDestinatario(v: String) = _yapeState.update { it.copy(destinatario = v) }
    fun setYapeCategoria(id: Long)   = _yapeState.update { it.copy(categoriaId = id) }

    fun guardarGastoYape() {
        val s = _yapeState.value
        val monto = s.montoDetectado.replace(",", ".").toDoubleOrNull()
        if (monto == null || monto <= 0) {
            _yapeState.update { it.copy(error = "Ingresa un monto válido") }
            return
        }
        val yapeCatId = s.categorias.find { it.nombre.contains("Yape", ignoreCase = true) }?.id
            ?: s.categoriaId

        viewModelScope.launch {
            _yapeState.update { it.copy(isSaving = true, error = null) }
            try {
                repo.insertGasto(
                    Gasto(
                        monto              = monto,
                        categoriaId        = yapeCatId,
                        descripcion        = if (s.destinatario.isNotBlank()) "Yape a ${s.destinatario}" else "Pago Yape",
                        fecha              = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        fuente             = "yape",
                        yapeDestinatario   = s.destinatario.ifBlank { null }
                    )
                )
                _yapeState.update { it.copy(isSaving = false, saved = true, imageUri = null, montoDetectado = "", destinatario = "") }
            } catch (e: Exception) {
                _yapeState.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }
}
