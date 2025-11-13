package com.example.myapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Activity que demuestra el uso de Services y Threads
 * Permite iniciar/detener una descarga simulada en segundo plano
 */
class DescargaActivity : AppCompatActivity() {

    private lateinit var btnIniciarDescarga: Button
    private lateinit var btnDetenerDescarga: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEstado: TextView
    private lateinit var tvProgreso: TextView
    private lateinit var btnVolver: ImageButton

    private var isServiceRunning = false

    // Receiver para recibir actualizaciones del servicio
    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val progress = intent?.getIntExtra(DownloadService.EXTRA_PROGRESS, 0) ?: 0
            val status = intent?.getStringExtra(DownloadService.EXTRA_STATUS) ?: ""

            android.util.Log.d("DescargaActivity", "📩 Broadcast recibido: $progress% - $status")
            actualizarUI(progress, status)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_descarga)

        // Inicializar vistas
        btnIniciarDescarga = findViewById(R.id.btnIniciarDescarga)
        btnDetenerDescarga = findViewById(R.id.btnDetenerDescarga)
        progressBar = findViewById(R.id.progressBar)
        tvEstado = findViewById(R.id.tvEstado)
        tvProgreso = findViewById(R.id.tvProgreso)
        btnVolver = findViewById(R.id.btnVolver)

        // Estado inicial
        btnDetenerDescarga.isEnabled = false
        progressBar.progress = 0
        tvProgreso.text = "0%"
        tvEstado.text = "Presiona 'Iniciar Descarga' para comenzar"

        // Botón volver
        btnVolver.setOnClickListener {
            finish()
        }

        // Iniciar descarga
        btnIniciarDescarga.setOnClickListener {
            if (!isServiceRunning) {
                iniciarServicioDescarga()
            }
        }

        // Detener descarga
        btnDetenerDescarga.setOnClickListener {
            if (isServiceRunning) {
                detenerServicioDescarga()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(DownloadService.ACTION_UPDATE)
        ContextCompat.registerReceiver(
            this,
            downloadReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        // Desregistrar el receiver
        try {
            unregisterReceiver(downloadReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun iniciarServicioDescarga() {
        val intent = Intent(this, DownloadService::class.java)
        startService(intent)

        isServiceRunning = true
        btnIniciarDescarga.isEnabled = false
        btnDetenerDescarga.isEnabled = true

        tvEstado.text = "Iniciando descarga..."
    }

    private fun detenerServicioDescarga() {
        val intent = Intent(this, DownloadService::class.java)
        stopService(intent)

        isServiceRunning = false
        btnIniciarDescarga.isEnabled = true
        btnDetenerDescarga.isEnabled = false
    }

    private fun actualizarUI(progress: Int, status: String) {
        progressBar.progress = progress
        tvProgreso.text = "$progress%"
        tvEstado.text = status

        // Si la descarga se completó o se detuvo, actualizar botones
        if (progress >= 100 || status.contains("detenida")) {
            isServiceRunning = false
            btnIniciarDescarga.isEnabled = true
            btnDetenerDescarga.isEnabled = false
        }
    }
}