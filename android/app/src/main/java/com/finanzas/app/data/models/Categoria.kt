package com.finanzas.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Categoria(
    val id: Long = 0,
    val nombre: String = "",
    val color: String = "#607D8B",
    val icono: String = "more_horiz",
    @SerialName("created_at")
    val createdAt: String = ""
)
