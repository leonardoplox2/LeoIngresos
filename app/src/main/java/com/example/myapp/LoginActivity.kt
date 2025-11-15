package com.example.myapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.tusistema.DBhelper
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    // 🔥 FIREBASE AUTH
    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // 🔥 LAUNCHER para Google Sign-In
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: Exception) {
            Log.e("LoginActivity", "Error en Google Sign-In: ${e.message}")
            Toast.makeText(this, "Error al iniciar sesión con Google", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activitylogin)

        // 🔥 INICIALIZAR FIREBASE
        auth = FirebaseAuth.getInstance()

        // 🔥 CONFIGURAR GOOGLE SIGN-IN
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // VISTAS EXISTENTES
        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvCrearCuenta = findViewById<TextView>(R.id.tvCrearCuenta)

        // 🔥 NUEVO BOTÓN DE GOOGLE SIGN-IN (lo agregaremos al XML después)
        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)

        val dbHelper = DBhelper(this)

        // LOGIN TRADICIONAL (tu código existente)
        btnLogin.setOnClickListener {
            val usuario = etUsuario.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.verificarUsuario(usuario, password)) {
                val datos = dbHelper.obtenerUsuario(usuario)
                val nombres = datos?.get("nombre_apellido")?.split(" ") ?: listOf("")
                val nombre = nombres.getOrElse(0) { "" }
                val apellido = nombres.drop(1).joinToString(" ")

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

        // 🔥 LOGIN CON GOOGLE
        btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }

        tvCrearCuenta.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // 🔥 AUTENTICACIÓN CON FIREBASE USANDO GOOGLE
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Guardar datos en SharedPreferences
                        val nombres = user.displayName?.split(" ") ?: listOf("Usuario", "")
                        val nombre = nombres.getOrElse(0) { "Usuario" }
                        val apellido = nombres.drop(1).joinToString(" ")

                        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        prefs.edit()
                            .clear()
                            .putString("usuario", user.email ?: "")
                            .putString("nombre", nombre)
                            .putString("apellido", apellido)
                            .putString("correo", user.email ?: "")
                            .putString("numero", "")
                            .putString("dni", "")
                            .putBoolean("isGoogleAuth", true) // 🔥 Marcamos que es login de Google
                            .apply()

                        Toast.makeText(this, "¡Bienvenido $nombre!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Log.e("LoginActivity", "Error en Firebase Auth: ${task.exception?.message}")
                    Toast.makeText(this, "Error al autenticar con Firebase", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        // 🔥 Verificar si ya hay un usuario autenticado con Google
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Ya hay sesión de Google activa
            val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val isGoogleAuth = prefs.getBoolean("isGoogleAuth", false)

            if (isGoogleAuth) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}