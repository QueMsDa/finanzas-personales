package com.finanzas.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Gasto(
    val id: Long = 0,
    val monto: Double,
    @SerialName("categoria_id")
    val categoriaId: Long,
    val descripcion: String? = null,
    val fecha: String = "",
    val fuente: String = "manual",
    val tipo: String = "personal",
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("yape_destinatario")
    val yapeDestinatario: String? = null,
    @SerialName("created_at")
    val createdAt: String = ""
)

@Serializable
data class GastoConCategoria(
    val id: Long = 0,
    val monto: Double = 0.0,
    @SerialName("categoria_id")
    val categoriaId: Long = 0,
    val descripcion: String? = null,
    val fecha: String = "",
    val fuente: String = "manual",
    val tipo: String = "personal",
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("yape_destinatario")
    val yapeDestinatario: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
    val categorias: Categoria? = null,
    val profiles: Perfil? = null
) {
    val nombreCategoria get() = categorias?.nombre ?: "Sin categoría"
    val colorCategoria  get() = categorias?.color  ?: "#607D8B"
    val esCompartido    get() = tipo == "compartido"
    val autor           get() = profiles?.nombre
}

@Serializable
data class Perfil(
    val id: String = "",
    val nombre: String = ""
)
