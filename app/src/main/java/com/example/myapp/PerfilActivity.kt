package com.example.myapp

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapp.DBhelper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

// ---------------- PERFIL ACTIVITY ----------------
class PerfilActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var dbHelper: DBhelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        dbHelper = DBhelper(this)
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        val usuario = prefs.getString("usuario", null)
        if (usuario == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 🗺️ Botón para abrir el mapa GPS
        findViewById<Button>(R.id.btnObtenerUbicacion).setOnClickListener {
            val intent = Intent(this, MapaGPSActivity::class.java)
            startActivity(intent)
        }

        // Botón Editar Perfil
        findViewById<Button>(R.id.btnEditarPerfil).setOnClickListener {
            startActivity(Intent(this, EditarPerfilActivity::class.java))
        }

        // Botón volver
        findViewById<ImageView>(R.id.imgBack).setOnClickListener {
            finish()
        }

        // 🔥 Botón cerrar sesión
        findViewById<Button>(R.id.btnCerrarSesionPerfil).setOnClickListener {
            val isGoogleAuth = prefs.getBoolean("isGoogleAuth", false)

            if (isGoogleAuth) {
                FirebaseAuth.getInstance().signOut()

                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(this, gso)
                googleSignInClient.signOut()
            }

            prefs.edit().clear().apply()
            finish()

            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_perfil, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_configuracion -> {
                Toast.makeText(this, "⚙️ Ir a Configuración", Toast.LENGTH_SHORT).show()
                true
            }
            R.id.menu_menu_principal -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_exportar_datos -> {
                Toast.makeText(this, "💾 Exportando Datos...", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarDatosPerfil()
    }

    private fun cargarDatosPerfil() {
        val usuario = prefs.getString("usuario", null) ?: return
        val isGoogleAuth = prefs.getBoolean("isGoogleAuth", false)

        android.util.Log.d("PerfilActivity", "════════════════════════════════")
        android.util.Log.d("PerfilActivity", "📱 CARGANDO PERFIL")
        android.util.Log.d("PerfilActivity", "👤 Usuario: $usuario")
        android.util.Log.d("PerfilActivity", "🔐 Google Auth: $isGoogleAuth")

        // 🔥 PASO 1: Leer de SharedPreferences PRIMERO
        val correoPrefs = prefs.getString("correo", "")
        android.util.Log.d("PerfilActivity", "📋 Correo en SharedPrefs: '$correoPrefs'")

        // 🔥 PASO 2: Intentar cargar de SQLite
        val datosSQLite = dbHelper.obtenerUsuario(usuario)
        android.util.Log.d("PerfilActivity", "🗄️ Datos en SQLite: ${datosSQLite != null}")

        if (datosSQLite != null) {
            android.util.Log.d("PerfilActivity", "   - nombre_apellido: ${datosSQLite["nombre_apellido"]}")
            android.util.Log.d("PerfilActivity", "   - numero: ${datosSQLite["numero"]}")
            android.util.Log.d("PerfilActivity", "   - dni: ${datosSQLite["dni"]}")
            android.util.Log.d("PerfilActivity", "   - correo: '${datosSQLite["correo"]}'")
        }

        val nombre: String
        val apellido: String
        val numero: String
        val dni: String
        val correo: String

        if (datosSQLite != null) {
            // ✅ Usar datos de SQLite
            val nombreCompleto = datosSQLite["nombre_apellido"] ?: ""
            val partes = nombreCompleto.split(" ")
            nombre = partes.getOrElse(0) { "" }
            apellido = partes.drop(1).joinToString(" ")
            numero = datosSQLite["numero"] ?: ""
            dni = datosSQLite["dni"] ?: ""

            // 🔥 Si el correo en SQLite está vacío pero es Google Auth, usar el usuario (email)
            val correoSQLite = datosSQLite["correo"] ?: ""
            correo = if (correoSQLite.isBlank() && isGoogleAuth) {
                android.util.Log.d("PerfilActivity", "⚠️ Correo vacío en SQLite, usando usuario: $usuario")
                usuario
            } else {
                correoSQLite
            }

        } else {
            // ❌ No hay datos en SQLite, usar SharedPreferences
            android.util.Log.d("PerfilActivity", "⚠️ No hay datos en SQLite, usando SharedPreferences")
            nombre = prefs.getString("nombre", "") ?: ""
            apellido = prefs.getString("apellido", "") ?: ""
            numero = prefs.getString("numero", "") ?: ""
            dni = prefs.getString("dni", "") ?: ""

            // 🔥 Si el correo en SharedPrefs está vacío pero es Google Auth, usar el usuario
            val correoSP = prefs.getString("correo", "") ?: ""
            correo = if (correoSP.isBlank() && isGoogleAuth) {
                android.util.Log.d("PerfilActivity", "⚠️ Correo vacío en SharedPrefs, usando usuario: $usuario")
                usuario
            } else {
                correoSP
            }
        }

        android.util.Log.d("PerfilActivity", "✅ Datos finales a mostrar:")
        android.util.Log.d("PerfilActivity", "   - Nombre: $nombre")
        android.util.Log.d("PerfilActivity", "   - Apellido: $apellido")
        android.util.Log.d("PerfilActivity", "   - Número: $numero")
        android.util.Log.d("PerfilActivity", "   - DNI: $dni")
        android.util.Log.d("PerfilActivity", "   - Correo: '$correo'")
        android.util.Log.d("PerfilActivity", "════════════════════════════════")

        // 🔥 Sincronizar SharedPreferences con los datos finales
        prefs.edit()
            .putString("nombre", nombre)
            .putString("apellido", apellido)
            .putString("numero", numero)
            .putString("dni", dni)
            .putString("correo", correo)
            .apply()

        // 🔥 Mostrar en la UI
        findViewById<TextView>(R.id.tvNombrePerfil).text = if (nombre.isNotBlank() || apellido.isNotBlank()) {
            "$nombre $apellido".trim()
        } else {
            "Usuario"
        }

        findViewById<TextView>(R.id.tvNumeroPerfil).text = "Número: ${if (numero.isBlank()) "-" else numero}"
        findViewById<TextView>(R.id.tvDniPerfil).text = "DNI: ${if (dni.isBlank()) "-" else dni}"
        findViewById<TextView>(R.id.tvCorreoPerfil).text = "Correo: ${if (correo.isBlank()) "-" else correo}"
    }
}

// ---------------- EDITAR PERFIL ACTIVITY ----------------
class EditarPerfilActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var dbHelper: DBhelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editar_perfil)

        dbHelper = DBhelper(this)
        prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val usuario = prefs.getString("usuario", null)
        val isGoogleAuth = prefs.getBoolean("isGoogleAuth", false)

        if (usuario == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etNumero = findViewById<EditText>(R.id.etNumero)
        val etDni = findViewById<EditText>(R.id.etDni)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val btnGuardar = findViewById<Button>(R.id.btnGuardar)

        // 🔥 Cargar datos actuales
        val datosSQLite = dbHelper.obtenerUsuario(usuario)

        if (datosSQLite != null) {
            val nombreCompleto = datosSQLite["nombre_apellido"] ?: ""
            val partes = nombreCompleto.split(" ")
            etNombre.setText(partes.getOrElse(0) { "" })
            etApellido.setText(partes.drop(1).joinToString(" "))
            etNumero.setText(datosSQLite["numero"] ?: "")
            etDni.setText(datosSQLite["dni"] ?: "")

            // 🔥 Si el correo está vacío en SQLite y es Google Auth, usar el usuario
            val correoSQLite = datosSQLite["correo"] ?: ""
            etCorreo.setText(if (correoSQLite.isBlank() && isGoogleAuth) usuario else correoSQLite)
        } else {
            etNombre.setText(prefs.getString("nombre", ""))
            etApellido.setText(prefs.getString("apellido", ""))
            etNumero.setText(prefs.getString("numero", ""))
            etDni.setText(prefs.getString("dni", ""))

            // 🔥 Si el correo está vacío en SharedPrefs y es Google Auth, usar el usuario
            val correoPrefs = prefs.getString("correo", "") ?: ""
            etCorreo.setText(if (correoPrefs.isBlank() && isGoogleAuth) usuario else correoPrefs)
        }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val numero = etNumero.text.toString().trim()
            val dni = etDni.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val nombreCompleto = "$nombre $apellido"

            if (nombre.isEmpty() || apellido.isEmpty()) {
                Toast.makeText(this, "❌ El nombre y apellido son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val actualizado = dbHelper.actualizarUsuario(usuario, nombreCompleto, numero, dni, correo)

            if (actualizado) {
                prefs.edit()
                    .putString("nombre", nombre)
                    .putString("apellido", apellido)
                    .putString("numero", numero)
                    .putString("dni", dni)
                    .putString("correo", correo)
                    .apply()

                android.util.Log.d("EditarPerfil", "✅ Datos actualizados")
                Toast.makeText(this, "✅ Datos actualizados correctamente", Toast.LENGTH_SHORT).show()

                setResult(RESULT_OK)
                finish()
            } else {
                android.util.Log.e("EditarPerfil", "❌ Error al actualizar")
                Toast.makeText(this, "❌ Error al actualizar. Verifica que los datos no estén duplicados", Toast.LENGTH_LONG).show()
            }
        }
    }
}