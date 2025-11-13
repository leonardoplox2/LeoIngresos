package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.tusistema.DBhelper

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitylogin)

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvCrearCuenta = findViewById<TextView>(R.id.tvCrearCuenta)

        val dbHelper = DBhelper(this)

        btnLogin.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.verificarUsuario(usuario, password)) {
                // Obtener todos los datos del usuario
                val datos = dbHelper.obtenerUsuario(usuario)

                val nombres = datos?.get("nombre_apellido")?.split(" ") ?: listOf("")
                val nombre = nombres.getOrElse(0) { "" }
                val apellido = nombres.drop(1).joinToString(" ")

                // Guardar en SharedPreferences
                val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                prefs.edit()
                    .clear()
                    .putString("usuario", usuario)
                    .putString("nombre", nombre)
                    .putString("apellido", apellido)
                    .putString("numero", datos?.get("numero") ?: "")
                    .putString("dni", datos?.get("dni") ?: "")
                    .putString("correo", datos?.get("correo") ?: "")
                    .apply()

                Toast.makeText(this, "Bienvenido $nombre", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }

        tvCrearCuenta.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
}
