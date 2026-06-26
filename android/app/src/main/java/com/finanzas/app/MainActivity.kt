package com.finanzas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.finanzas.app.ui.navigation.FinanzasNavGraph
import com.finanzas.app.ui.theme.FinanzasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FinanzasTheme {
                FinanzasNavGraph()
            }
        }
    }
}
