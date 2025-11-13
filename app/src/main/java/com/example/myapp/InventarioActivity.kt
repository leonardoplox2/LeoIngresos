package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class InventarioActivity : AppCompatActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var imgAvatar: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inventario)

        // Header
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val nombre = prefs.getString("nombre", "Usuario")
        val apellido = prefs.getString("apellido", "")
        tvNombre = findViewById(R.id.tvBienvenido)
        tvNombre.text = "$nombre $apellido"

        imgAvatar = findViewById(R.id.imgAvatar)
        imgAvatar.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        // Footer
        val btnHome = findViewById<LinearLayout>(R.id.btnHome)
        val btnBalance = findViewById<LinearLayout>(R.id.btnBalance)
        val btnInventario = findViewById<LinearLayout>(R.id.btnInventario)

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        btnBalance.setOnClickListener {
            val intent = Intent(this, BalanceActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        btnInventario.setOnClickListener {
            // Ya estamos en Inventario, no hacemos nada
        }
    }
}
