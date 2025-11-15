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

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

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

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvCrearCuenta = findViewById<TextView>(R.id.tvCrearCuenta)
        val btnGoogleSignIn = findViewById<Button>(R.id.btnGoogleSignIn)

        val dbHelper = DBhelper(this)

        // LOGIN TRADICIONAL
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
                    .putBoolean("isGoogleAuth", false)
                    .apply()

                Toast.makeText(this, "Bienvenido $nombre", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Usuario o contraseña incorrectos", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }

        tvCrearCuenta.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val email = user.email ?: ""
                        val displayName = user.displayName ?: "Usuario Google"

                        Log.d("LoginActivity", "════════════════════════════════")
                        Log.d("LoginActivity", "🔥 INICIO DE SESIÓN CON GOOGLE")
                        Log.d("LoginActivity", "📧 Email: $email")
                        Log.d("LoginActivity", "👤 Nombre: $displayName")
                        Log.d("LoginActivity", "════════════════════════════════")

                        val dbHelper = DBhelper(this)

                        // 🔥 PASO 1: Verificar si el usuario existe
                        var usuarioExistente = dbHelper.obtenerUsuario(email)
                        Log.d("LoginActivity", "📊 Usuario existente en DB: ${usuarioExistente != null}")

                        if (usuarioExistente == null) {
                            // 🔥 PASO 2: Crear usuario nuevo
                            Log.d("LoginActivity", "➕ Creando nuevo usuario en SQLite...")
                            val resultado = dbHelper.insertarUsuario(
                                nombreApellido = displayName,
                                usuario = email,
                                password = "GOOGLE_AUTH_${user.uid}",
                                numero = ""
                            )

                            if (resultado) {
                                Log.d("LoginActivity", "✅ Usuario creado exitosamente")
                            } else {
                                Log.e("LoginActivity", "❌ Error al crear usuario")
                            }
                        }

                        // 🔥 PASO 3: FORZAR actualización del correo (siempre, incluso si ya existe)
                        Log.d("LoginActivity", "🔄 Actualizando correo en SQLite...")
                        val actualizado = dbHelper.actualizarUsuario(
                            usuario = email,
                            nombreApellido = displayName,
                            numero = "",
                            dni = "",
                            correo = email  // 🔥 IMPORTANTE: Asegurar que el correo se guarde
                        )

                        if (actualizado) {
                            Log.d("LoginActivity", "✅ Correo actualizado en SQLite: $email")
                        } else {
                            Log.e("LoginActivity", "❌ Error al actualizar correo en SQLite")
                        }

                        // 🔥 PASO 4: Verificar que el correo se guardó correctamente
                        usuarioExistente = dbHelper.obtenerUsuario(email)
                        val correoGuardado = usuarioExistente?.get("correo") ?: ""
                        Log.d("LoginActivity", "🔍 Verificación - Correo guardado: '$correoGuardado'")

                        // Separar nombre y apellido
                        val nombres = displayName.split(" ")
                        val nombre = nombres.getOrElse(0) { "Usuario" }
                        val apellido = nombres.drop(1).joinToString(" ")

                        // 🔥 PASO 5: Guardar en SharedPreferences
                        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        prefs.edit()
                            .clear()
                            .putString("usuario", email)
                            .putString("nombre", nombre)
                            .putString("apellido", apellido)
                            .putString("correo", email)  // 🔥 Guardar correo
                            .putString("numero", "")
                            .putString("dni", "")
                            .putBoolean("isGoogleAuth", true)
                            .apply()

                        // 🔥 PASO 6: Verificar que se guardó en SharedPreferences
                        val correoPrefs = prefs.getString("correo", "NO_GUARDADO")
                        Log.d("LoginActivity", "🔍 Verificación - Correo en SharedPrefs: '$correoPrefs'")
                        Log.d("LoginActivity", "════════════════════════════════")

                        Toast.makeText(this, "¡Bienvenido $nombre!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                } else {
                    Log.e("LoginActivity", "❌ Error en Firebase Auth: ${task.exception?.message}")
                    Toast.makeText(this, "Error al autenticar con Firebase: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun onStart() {
        super.onStart()
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val usuario = prefs.getString("usuario", null)

        if (usuario != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}