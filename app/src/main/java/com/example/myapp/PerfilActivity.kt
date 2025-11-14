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
import com.example.tusistema.DBhelper
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
// ---------------- PERFIL ACTIVITY ----------------
class PerfilActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var dbHelper: DBhelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar) // Necesario para que Android gestione el menú
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

        // Botón cerrar sesión
        findViewById<Button>(R.id.btnCerrarSesionPerfil).setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // ESTA FUNCIÓN HACE QUE EL MENÚ APAREZCA
        menuInflater.inflate(R.menu.menu_perfil, menu)
        return true
    }

    /** Maneja la selección de ítems del menú (⋮) */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.menu_configuracion -> {
                Toast.makeText(this, "⚙️ Ir a Configuración", Toast.LENGTH_SHORT).show()
                true
            }

            // CÓDIGO ACTUALIZADO PARA IR A MAINACTIVITY
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
        val datos = dbHelper.obtenerUsuario(usuario)

        val nombre = datos?.get("nombre_apellido")?.split(" ")?.getOrElse(0) { "" } ?: ""
        val apellido = datos?.get("nombre_apellido")?.split(" ")?.drop(1)?.joinToString(" ") ?: ""
        val numero = datos?.get("numero") ?: "-"
        val dni = datos?.get("dni") ?: "-"
        val correo = datos?.get("correo") ?: "-"

        // Actualizar SharedPreferences
        prefs.edit()
            .putString("nombre", nombre)
            .putString("apellido", apellido)
            .putString("numero", numero)
            .putString("dni", dni)
            .putString("correo", correo)
            .apply()

        // Mostrar datos en TextViews
        findViewById<TextView>(R.id.tvNombrePerfil).text = "$nombre $apellido"
        findViewById<TextView>(R.id.tvNumeroPerfil).text = "Número: $numero"
        findViewById<TextView>(R.id.tvDniPerfil).text = "DNI: $dni"
        findViewById<TextView>(R.id.tvCorreoPerfil).text = "Correo: $correo"
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

            val actualizado = dbHelper.actualizarUsuario(usuario, nombreCompleto, numero, dni, correo)
            if (actualizado) {
                // Actualizar SharedPreferences
                prefs.edit()
                    .putString("nombre", nombre)
                    .putString("apellido", apellido)
                    .putString("numero", numero)
                    .putString("dni", dni)
                    .putString("correo", correo)
                    .apply()

                Toast.makeText(this, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Error al actualizar los datos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}