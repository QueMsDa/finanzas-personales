package com.finanzas.app.data.repository

import com.finanzas.app.data.SupabaseConfig
import com.finanzas.app.data.models.Categoria
import com.finanzas.app.data.models.Gasto
import com.finanzas.app.data.models.GastoConCategoria
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class GastoRepository {

    private val client = SupabaseConfig.client

    suspend fun getCategorias(): List<Categoria> =
        client.from("categorias").select().decodeList()

    suspend fun getGastosMesActual(): List<GastoConCategoria> {
        val ym    = YearMonth.now()
        val desde = ym.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val hasta = ym.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE)
        return client.from("gastos")
            .select(Columns.raw("*, categorias(*)")) {
                filter {
                    gte("fecha", desde)
                    lte("fecha", "${hasta}T23:59:59")
                }
                order("fecha", Order.DESCENDING)
            }
            .decodeList()
    }

    suspend fun getTodosLosGastos(): List<GastoConCategoria> =
        client.from("gastos")
            .select(Columns.raw("*, categorias(*)")) {
                order("fecha", Order.DESCENDING)
            }
            .decodeList()

    suspend fun insertGasto(gasto: Gasto): GastoConCategoria =
        client.from("gastos")
            .insert(gasto) {
                select(Columns.raw("*, categorias(*)"))
            }
            .decodeSingle()

    suspend fun deleteGasto(id: Long) {
        client.from("gastos").delete {
            filter { eq("id", id) }
        }
    }

    fun observarCambios(): Flow<PostgresAction> {
        val channel = client.channel("gastos-realtime")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "gastos"
        }
    }
}
