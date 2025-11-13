package com.example.myapp

import android.content.Context
import android.media.MediaPlayer

/**
 * Clase para gestionar los sonidos de la aplicación
 */
object SonidoManager {

    private var mediaPlayerVenta: MediaPlayer? = null
    private var mediaPlayerGasto: MediaPlayer? = null

    /**
     * Reproduce el sonido de venta exitosa
     */
    fun reproducirSonidoVenta(context: Context) {
        try {
            // Liberar el reproductor anterior si existe
            mediaPlayerVenta?.release()

            // Crear nuevo reproductor con el sonido de venta
            mediaPlayerVenta = MediaPlayer.create(context, R.raw.sonido_venta)
            mediaPlayerVenta?.start()

            // Liberar recursos cuando termine de reproducir
            mediaPlayerVenta?.setOnCompletionListener { mp ->
                mp.release()
                mediaPlayerVenta = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Si no encuentra el archivo, usa sonido del sistema
            reproducirSonidoSistema(context)
        }
    }

    /**
     * Reproduce el sonido de gasto registrado
     */
    fun reproducirSonidoGasto(context: Context) {
        try {
            // Liberar el reproductor anterior si existe
            mediaPlayerGasto?.release()

            // Crear nuevo reproductor con el sonido de gasto
            mediaPlayerGasto = MediaPlayer.create(context, R.raw.sonido_gasto)
            mediaPlayerGasto?.start()

            // Liberar recursos cuando termine de reproducir
            mediaPlayerGasto?.setOnCompletionListener { mp ->
                mp.release()
                mediaPlayerGasto = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Si no encuentra el archivo, usa sonido del sistema
            reproducirSonidoSistema(context)
        }
    }

    /**
     * Reproduce un sonido del sistema como alternativa
     */
    private fun reproducirSonidoSistema(context: Context) {
        try {
            // Usar RingtoneManager para obtener un sonido de notificación del sistema
            val notification = android.media.RingtoneManager.getDefaultUri(
                android.media.RingtoneManager.TYPE_NOTIFICATION
            )
            val ringtone = android.media.RingtoneManager.getRingtone(context, notification)
            ringtone?.play()
        } catch (e: Exception) {
            e.printStackTrace()
            // Si falla, no hacer nada (silencioso)
        }
    }

    /**
     * Libera todos los recursos de audio
     */
    fun liberarRecursos() {
        try {
            mediaPlayerVenta?.release()
            mediaPlayerGasto?.release()
            mediaPlayerVenta = null
            mediaPlayerGasto = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}