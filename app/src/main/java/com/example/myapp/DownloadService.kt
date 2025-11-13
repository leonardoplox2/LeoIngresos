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
        isDownloading = true
        currentProgress = 0

        Log.d(TAG, "🚀 Servicio de descarga iniciado...")

        // Simulación de descarga con un hilo (Thread)
        thread {
            var progress = 0

            while (isDownloading && progress <= 100) {
                Thread.sleep(1000) // Simula 1 segundo de descarga por cada 10%

                if (isDownloading) {
                    currentProgress = progress
                    Log.d(TAG, "📥 Descargando... $progress%")

                    // Enviar broadcast para actualizar la UI
                    enviarActualizacion(progress, "Descargando... $progress%")

                    progress += 10
                }
            }

            if (progress >= 100 && isDownloading) {
                Log.d(TAG, "✅ Descarga completada al 100%")
                enviarActualizacion(100, "✅ Descarga completada")
                stopSelf() // Detiene el servicio al terminar
            }
        }

        return START_STICKY
    }

    /**
     * Envía un broadcast con el progreso actual
     */
    private fun enviarActualizacion(progress: Int, status: String) {
        val intent = Intent(ACTION_UPDATE).apply {
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_STATUS, status)
        }
        sendBroadcast(intent)
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