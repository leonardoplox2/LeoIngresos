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
import com.example.myapp.DBhelper
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
            Toast.makeText(this, "Error al iniciar sesión con Google: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    .putBoolean("isGoogleAuth", false) // 🔥 Login tradicional
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
                        // 🔥 GUARDAR EN SQLITE TAMBIÉN
                        val dbHelper = DBhelper(this)
                        val email = user.email ?: ""
                        val displayName = user.displayName ?: "Usuario Google"

                        // Verificar si ya existe en la base de datos
                        val usuarioExistente = dbHelper.obtenerUsuario(email)

                        if (usuarioExistente == null) {
                            // 🔥 CREAR USUARIO EN SQLITE
                            val resultado = dbHelper.insertarUsuario(
                                nombreApellido = displayName,
                                usuario = email,
                                password = "GOOGLE_AUTH_${user.uid}", // Password especial para usuarios de Google
                                numero = ""
                            )

                            if (!resultado) {
                                Log.w("LoginActivity", "No se pudo crear usuario en SQLite")
                            }
                        }

                        // Separar nombre y apellido
                        val nombres = displayName.split(" ")
                        val nombre = nombres.getOrElse(0) { "Usuario" }
                        val apellido = nombres.drop(1).joinToString(" ")

                        // Guardar en SharedPreferences
                        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        prefs.edit()
                            .clear()
                            .putString("usuario", email)
                            .putString("nombre", nombre)
                            .putString("apellido", apellido)
                            .putString("correo", email)
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
                    Toast.makeText(this, "Error al autenticar con Firebase: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        // 🔥 Verificar si ya hay un usuario autenticado
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val usuario = prefs.getString("usuario", null)

        if (usuario != null) {
            // Ya hay sesión activa, ir a MainActivity
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}