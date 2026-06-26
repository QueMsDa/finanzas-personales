package com.finanzas.app.ocr

import android.net.Uri
import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class YapeResult(
    val monto: Double?,
    val destinatario: String?,
    val textoCompleto: String
)

object YapeRecognizer {

    // Patrones típicos de Yape Perú:
    // "Enviaste S/ 50.00", "S/ 150.00", "S/. 25", "PEN 30.00"
    private val MONTO_REGEX = Regex(
        """(?:S/\.?\s*|PEN\s*)(\d{1,6}(?:[.,]\d{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // El destinatario suele aparecer después de "Para:", "A:", "Enviaste a"
    private val DESTINATARIO_REGEX = Regex(
        """(?:Para|A|Enviaste\s+a|a)\s*[:\s]\s*([A-ZÁÉÍÓÚÑ][a-záéíóúñ]+(?:\s+[A-ZÁÉÍÓÚÑ][a-záéíóúñ]+)*)""",
        RegexOption.IGNORE_CASE
    )

    suspend fun reconocer(context: Context, uri: Uri): YapeResult {
        val image       = InputImage.fromFilePath(context, uri)
        val recognizer  = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val visionText = suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }

        val texto = visionText.text

        // Buscar todos los montos y quedarse con el mayor (el del encabezado de Yape)
        val montos = MONTO_REGEX.findAll(texto).mapNotNull { match ->
            match.groupValues[1].replace(",", ".").toDoubleOrNull()
        }.toList()

        val monto        = montos.maxOrNull()
        val destinatario = DESTINATARIO_REGEX.find(texto)?.groupValues?.getOrNull(1)?.trim()

        return YapeResult(
            monto        = monto,
            destinatario = destinatario,
            textoCompleto = texto
        )
    }
}
