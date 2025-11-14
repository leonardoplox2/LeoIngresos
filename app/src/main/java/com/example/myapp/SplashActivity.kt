package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    // Duración del splash en milisegundos (3 segundos)
    private val SPLASH_DURATION = 3000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Aplicar animaciones
        aplicarAnimaciones()

        // Verificar si el usuario ya ha iniciado sesión
        Handler(Looper.getMainLooper()).postDelayed({
            verificarSesion()
        }, SPLASH_DURATION)
    }

    private fun aplicarAnimaciones() {
        // Obtener las vistas
        val imgLogo = findViewById<ImageView>(R.id.imgLogo)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)

        // Crear animación de fade in (aparecer gradualmente)
        val fadeIn = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        fadeIn.duration = 1000

        // Aplicar animaciones
        imgLogo.startAnimation(fadeIn)

        // Retrasar la animación del texto
        Handler(Looper.getMainLooper()).postDelayed({
            tvAppName.startAnimation(fadeIn)
            tvSubtitle.startAnimation(fadeIn)
        }, 300)
    }

    private fun verificarSesion() {
        // Verificar si el usuario ya inició sesión
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val usuario = prefs.getString("usuario", null)

        val intent = if (usuario != null) {
            // Si hay sesión activa, ir a MainActivity
            Intent(this, MainActivity::class.java)
        } else {
            // Si no hay sesión, ir a LoginActivity
            Intent(this, LoginActivity::class.java)
        }

        startActivity(intent)
        finish() // Cerrar el splash para que no se pueda volver con el botón atrás
    }

    // Desactivar el botón de retroceso durante el splash
    override fun onBackPressed() {
        // No hacer nada - evita que el usuario salga durante la carga
    }
}