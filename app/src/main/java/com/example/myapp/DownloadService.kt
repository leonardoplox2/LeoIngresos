package com.example.myapp

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlin.concurrent.thread

/**
 * Servicio que simula una descarga en segundo plano
 * Demuestra el uso de Services y Threads en Android
 */
class DownloadService : Service() {

    private var isDownloading = false
    private var currentProgress = 0

    companion object {
        const val ACTION_UPDATE = "com.example.myapp.DOWNLOAD_UPDATE"
        const val EXTRA_PROGRESS = "progress"
        const val EXTRA_STATUS = "status"
        const val TAG = "DownloadService"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (isDownloading) {
            Log.d(TAG, "⚠️ El servicio ya está ejecutándose")
            return START_STICKY
        }

        isDownloading = true
        currentProgress = 0

        Log.d(TAG, "🚀 Servicio de descarga iniciado...")
        enviarActualizacion(0, "Iniciando descarga...")

        // Simulación de descarga con un hilo (Thread)
        thread {
            try {
                var progress = 0

                while (isDownloading && progress <= 100) {
                    // Primero actualizar y LUEGO dormir
                    currentProgress = progress
                    Log.d(TAG, "📥 Descargando... $progress%")

                    // Enviar broadcast para actualizar la UI
                    enviarActualizacion(progress, "Descargando... $progress%")

                    // Si ya llegamos al 100%, salir
                    if (progress >= 100) {
                        Log.d(TAG, "✅ Descarga completada al 100%")
                        enviarActualizacion(100, "✅ Descarga completada")
                        break
                    }

                    // Incrementar progreso
                    progress += 10

                    // Dormir DESPUÉS de enviar
                    if (isDownloading) {
                        Thread.sleep(1000) // Simula 1 segundo de descarga
                    }
                }

                // Detener el servicio cuando termine
                Thread.sleep(500)
                stopSelf()

            } catch (e: Exception) {
                Log.e(TAG, "❌ Error en la descarga: ${e.message}")
                enviarActualizacion(currentProgress, "❌ Error en la descarga")
                stopSelf()
            }
        }

        return START_STICKY
    }

    /**
     * Envía un broadcast con el progreso actual
     */
    private fun enviarActualizacion(progress: Int, status: String) {
        try {
            val intent = Intent(ACTION_UPDATE).apply {
                putExtra(EXTRA_PROGRESS, progress)
                putExtra(EXTRA_STATUS, status)
            }
            sendBroadcast(intent)
            Log.d(TAG, "📡 Broadcast enviado: $progress% - $status")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al enviar broadcast: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isDownloading = false
        Log.d(TAG, "🛑 Servicio detenido.")

        // Notificar que el servicio fue detenido
        if (currentProgress < 100) {
            enviarActualizacion(currentProgress, "⏸️ Descarga detenida")
        }
    }
}