package com.finanzas.app

import android.app.Application
import com.finanzas.app.data.SupabaseConfig

class FinanzasApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseConfig.client  // inicializa el cliente lazy
    }
}
