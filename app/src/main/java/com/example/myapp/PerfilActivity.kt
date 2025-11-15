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

        // 🔥 Botón cerrar sesión - CORREGIDO
        findViewById<Button>(R.id.btnCerrarSesionPerfil).setOnClickListener {
            val isGoogleAuth = prefs.getBoolean("isGoogleAuth", false)

            if (isGoogleAuth) {
                // 🔥 Cerrar sesión de Firebase y Google
                FirebaseAuth.getInstance().signOut()

                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(this, gso)
                googleSignInClient.signOut()
            }

            // 🔥 Limpiar TODAS las SharedPreferences
            prefs.edit().clear().apply()

            // 🔥 IMPORTANTE: Limpiar también el caché de la actividad
            // Esto asegura que al volver a abrir, se recarguen los datos correctos
            finish()

            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()

            // Ir al login
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

        android.util.Log.d("PerfilActivity", "Cargando datos para usuario: $usuario")

        // 🔥 SIEMPRE CARGAR DE SQLITE PRIMERO (es la fuente de verdad)
        val datos = dbHelper.obtenerUsuario(usuario)

        if (datos != null) {
            android.util.Log.d("PerfilActivity", "Datos encontrados en SQLite: $datos")

            val nombre = datos["nombre_apellido"]?.split(" ")?.getOrElse(0) { "" } ?: ""
            val apellido = datos["nombre_apellido"]?.split(" ")?.drop(1)?.joinToString(" ") ?: ""
            val numero = datos["numero"] ?: ""
            val dni = datos["dni"] ?: ""
            val correo = datos["correo"] ?: ""

            android.util.Log.d("PerfilActivity", "Mostrando: $nombre $apellido | $numero | $dni | $correo")

            // 🔥 Actualizar SharedPreferences con los datos más recientes
            prefs.edit()
                .putString("nombre", nombre)
                .putString("apellido", apellido)
                .putString("numero", numero)
                .putString("dni", dni)
                .putString("correo", correo)
                .apply()

            // Mostrar datos en TextViews
            findViewById<TextView>(R.id.tvNombrePerfil).text = "$nombre $apellido"
            findViewById<TextView>(R.id.tvNumeroPerfil).text = "Número: ${if (numero.isBlank()) "-" else numero}"
            findViewById<TextView>(R.id.tvDniPerfil).text = "DNI: ${if (dni.isBlank()) "-" else dni}"
            findViewById<TextView>(R.id.tvCorreoPerfil).text = "Correo: ${if (correo.isBlank()) "-" else correo}"
        } else {
            android.util.Log.w("PerfilActivity", "No se encontraron datos en SQLite")

            // 🔥 Si no hay datos en SQLite, usar SharedPreferences (caso de error o datos temporales)
            val nombre = prefs.getString("nombre", "") ?: ""
            val apellido = prefs.getString("apellido", "") ?: ""
            val numero = prefs.getString("numero", "") ?: ""
            val dni = prefs.getString("dni", "") ?: ""
            val correo = prefs.getString("correo", "") ?: ""

            findViewById<TextView>(R.id.tvNombrePerfil).text = "$nombre $apellido"
            findViewById<TextView>(R.id.tvNumeroPerfil).text = "Número: ${if (numero.isBlank()) "-" else numero}"
            findViewById<TextView>(R.id.tvDniPerfil).text = "DNI: ${if (dni.isBlank()) "-" else dni}"
            findViewById<TextView>(R.id.tvCorreoPerfil).text = "Correo: ${if (correo.isBlank()) "-" else correo}"
        }
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

        // Cargar datos actuales desde SharedPreferences
        etNombre.setText(prefs.getString("nombre", ""))
        etApellido.setText(prefs.getString("apellido", ""))
        etNumero.setText(prefs.getString("numero", ""))
        etDni.setText(prefs.getString("dni", ""))
        etCorreo.setText(prefs.getString("correo", ""))

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val apellido = etApellido.text.toString().trim()
            val numero = etNumero.text.toString().trim()
            val dni = etDni.text.toString().trim()
            val correo = etCorreo.text.toString().trim()
            val nombreCompleto = "$nombre $apellido"

            // 🔥 VALIDAR CAMPOS VACÍOS
            if (nombre.isEmpty() || apellido.isEmpty()) {
                Toast.makeText(this, "❌ El nombre y apellido son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 🔥 VALIDAR DUPLICADOS
            if (numero.isNotBlank() && dbHelper.numeroExiste(numero, usuario)) {
                Toast.makeText(this, "❌ El número '$numero' ya está registrado con otra cuenta", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (dni.isNotBlank() && dbHelper.dniExiste(dni, usuario)) {
                Toast.makeText(this, "❌ El DNI '$dni' ya está registrado con otra cuenta", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (correo.isNotBlank() && dbHelper.correoExiste(correo, usuario)) {
                Toast.makeText(this, "❌ El correo '$correo' ya está registrado con otra cuenta", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 🔥 ACTUALIZAR EN SQLITE
            val actualizado = dbHelper.actualizarUsuario(usuario, nombreCompleto, numero, dni, correo)

            if (actualizado) {
                // 🔥 Actualizar SharedPreferences inmediatamente
                prefs.edit()
                    .putString("nombre", nombre)
                    .putString("apellido", apellido)
                    .putString("numero", numero)
                    .putString("dni", dni)
                    .putString("correo", correo)
                    .apply()

                // 🔥 DEBUG: Verificar que se guardó
                android.util.Log.d("EditarPerfil", "Datos actualizados en SQLite")
                android.util.Log.d("EditarPerfil", "Nombre: $nombre $apellido")
                android.util.Log.d("EditarPerfil", "Número: $numero")
                android.util.Log.d("EditarPerfil", "DNI: $dni")
                android.util.Log.d("EditarPerfil", "Correo: $correo")

                Toast.makeText(this, "✅ Datos actualizados correctamente", Toast.LENGTH_SHORT).show()

                // 🔥 Usar setResult para indicar que hubo cambios
                setResult(RESULT_OK)
                finish()
            } else {
                android.util.Log.e("EditarPerfil", "Error al actualizar en SQLite")
                Toast.makeText(this, "❌ Error al actualizar. Verifica que los datos no estén duplicados", Toast.LENGTH_LONG).show()
            }
        }
    }
}