package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapp.DBhelper

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etNombreApellido = findViewById<EditText>(R.id.etNombreApellido)
        val etUsuario = findViewById<EditText>(R.id.etUsuarioRegister)
        val etPassword = findViewById<EditText>(R.id.etPasswordRegister)
        val etRepetirPassword = findViewById<EditText>(R.id.etRepetirPassword)
        val etNumero = findViewById<EditText>(R.id.etNumero)
        val btnRegistrar = findViewById<Button>(R.id.btnRegister)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)

        val dbHelper = DBhelper(this)

        btnRegistrar.setOnClickListener {
            val nombreApellido = etNombreApellido.text.toString().trim()
            val usuario = etUsuario.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val repetirPassword = etRepetirPassword.text.toString().trim()
            val numero = etNumero.text.toString().trim()

            // 🔥 VALIDACIONES
            if (nombreApellido.isEmpty() || usuario.isEmpty() || password.isEmpty() ||
                repetirPassword.isEmpty() || numero.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != repetirPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 Validar longitud mínima de contraseña
            if (password.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 VERIFICAR SI EL USUARIO YA EXISTE
            if (dbHelper.usuarioExiste(usuario)) {
                Toast.makeText(this, "❌ El usuario '$usuario' ya está registrado", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 🔥 VERIFICAR SI EL NÚMERO YA EXISTE
            if (dbHelper.numeroExiste(numero)) {
                Toast.makeText(this, "❌ El número '$numero' ya está registrado con otra cuenta", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 🔥 INTENTAR REGISTRAR
            val insertado = dbHelper.insertarUsuario(nombreApellido, usuario, password, numero)

            if (insertado) {
                // Separar nombre y apellido
                val nombres = nombreApellido.split(" ")
                val nombre = nombres.getOrElse(0) { "" }
                val apellido = nombres.drop(1).joinToString(" ")

                // Guardar datos del nuevo usuario en SharedPreferences
                val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                prefs.edit()
                    .clear()
                    .putString("usuario", usuario)
                    .putString("nombre", nombre)
                    .putString("apellido", apellido)
                    .putString("numero", numero)
                    .putString("dni", "")
                    .putString("correo", "")
                    .putBoolean("isGoogleAuth", false) // 🔥 Login tradicional
                    .apply()

                Toast.makeText(this, "✅ Usuario registrado correctamente", Toast.LENGTH_SHORT).show()

                // Ir a MainActivity
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "❌ Error al registrar. El usuario ya existe", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}