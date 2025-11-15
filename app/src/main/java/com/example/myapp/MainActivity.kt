package com.example.myapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {

    private lateinit var tvBienvenido: TextView
    private lateinit var imgAvatar: ImageView

    private lateinit var overlayVenta: FrameLayout
    private lateinit var layoutOpcionesVenta: LinearLayout
    private lateinit var btnVentaLibre: Button
    private lateinit var btnVentaProductos: Button
    private lateinit var cardVenta: CardView
    private lateinit var cardGastos: CardView
    private lateinit var cardInventario: CardView
    private lateinit var cardDescarga: CardView

    // 🔹 PERSISTENCIA - AHORA CON USUARIO ESPECÍFICO
    private var listaMovimientos = mutableListOf<Movimiento>()
    private val PREFS_BALANCE = "BalancePrefs"
    // 🔥 KEY_MOVIMIENTOS ahora será dinámico por usuario (se inicializa en onCreate)
    private var keyMovimientosUsuario: String = ""

    private val registroMovimientoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data

            cargarMovimientos()

            val valor = data?.getDoubleExtra("valor", 0.0) ?: 0.0
            val conceptoVenta = data?.getStringExtra("concepto")

            val montoGasto = data?.getDoubleExtra("monto", 0.0) ?: 0.0
            val conceptoGasto = data?.getStringExtra("concepto")

            val fecha = data?.getStringExtra("fecha") ?: ""
            val metodoPago = data?.getStringExtra("metodoPago") ?: "Efectivo"

            if (valor > 0.0 && conceptoVenta != null) {
                val nuevoIngreso = Movimiento("Ingreso", conceptoVenta, valor, fecha, metodoPago)
                listaMovimientos.add(nuevoIngreso)
                guardarMovimientos()
                Toast.makeText(this, "✅ Venta registrada y guardada.", Toast.LENGTH_SHORT).show()

            } else if (montoGasto > 0.0 && conceptoGasto != null) {
                val nuevoEgreso = Movimiento("Egreso", conceptoGasto, montoGasto, fecha, metodoPago)
                listaMovimientos.add(nuevoEgreso)
                guardarMovimientos()
                Toast.makeText(this, "🛑 Gasto registrado y guardado.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🔥 OBTENER USUARIO ACTUAL Y CREAR KEY ÚNICA
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val usuario = prefs.getString("usuario", "default_user") ?: "default_user"

        // 🔥 Crear key única por usuario
        keyMovimientosUsuario = "movimientos_$usuario"

        // 1. INICIALIZACIÓN DE VISTAS
        tvBienvenido = findViewById(R.id.tvBienvenido)
        imgAvatar = findViewById(R.id.imgAvatar)
        cardVenta = findViewById(R.id.cardVenta)
        cardGastos = findViewById(R.id.cardGastos)
        cardInventario = findViewById(R.id.cardInventario)
        cardDescarga = findViewById(R.id.cardDescarga)
        overlayVenta = findViewById(R.id.overlayVenta)
        layoutOpcionesVenta = findViewById(R.id.layoutOpcionesVenta)
        btnVentaLibre = findViewById(R.id.btnVentaLibre)
        btnVentaProductos = findViewById(R.id.btnVentaProductos)
        val btnCerrarOverlay = findViewById<ImageView>(R.id.btnCerrarVenta)
        val btnHome = findViewById<LinearLayout>(R.id.btnHome)
        val btnBalance = findViewById<LinearLayout>(R.id.btnBalance)
        val btnInventarioFooter = findViewById<LinearLayout>(R.id.btnInventario)

        // 2. LÓGICA DE HEADER
        val nombre = prefs.getString("nombre", "Usuario")
        val apellido = prefs.getString("apellido", "")
        tvBienvenido.text = "Bienvenido $nombre $apellido 👋"

        imgAvatar.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        // 3. LÓGICA DE DASHBOARD Y MODAL
        cardVenta.setOnClickListener { mostrarModalVenta() }

        cardGastos.setOnClickListener {
            registroMovimientoLauncher.launch(Intent(this, NuevoGastoActivity::class.java))
        }

        cardInventario.setOnClickListener {
            val intent = Intent(this, InventarioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        cardDescarga.setOnClickListener {
            val intent = Intent(this, DescargaActivity::class.java)
            startActivity(intent)
        }

        btnCerrarOverlay.setOnClickListener { cerrarModalVenta() }

        btnVentaLibre.setOnClickListener {
            cerrarModalVenta()
            registroMovimientoLauncher.launch(Intent(this, VentaLibreActivity::class.java))
        }

        btnVentaProductos.setOnClickListener {
            cerrarModalVenta()
        }

        // 4. LÓGICA DEL FOOTER
        btnHome.setOnClickListener { }

        btnBalance.setOnClickListener {
            val intent = Intent(this, BalanceActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        btnInventarioFooter.setOnClickListener {
            val intent = Intent(this, InventarioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }
    }

    // ----------------------------------------------------
    // 🔹 FUNCIONES DE BALANCE Y PERSISTENCIA
    // ----------------------------------------------------

    private fun cargarMovimientos() {
        val prefs = getSharedPreferences(PREFS_BALANCE, MODE_PRIVATE)
        val json = prefs.getString(keyMovimientosUsuario, null)

        if (json != null) {
            val type = object : TypeToken<MutableList<Movimiento>>() {}.type
            listaMovimientos = Gson().fromJson(json, type)
        } else {
            listaMovimientos = mutableListOf()
        }
    }

    private fun guardarMovimientos() {
        val prefs = getSharedPreferences(PREFS_BALANCE, MODE_PRIVATE)
        val editor = prefs.edit()
        val json = Gson().toJson(listaMovimientos)
        editor.putString(keyMovimientosUsuario, json)
        editor.apply()
    }

    // ----------------------------------------------------
    // 🔹 FUNCIONES DE MODAL
    // ----------------------------------------------------
    private fun mostrarModalVenta() {
        overlayVenta.visibility = View.VISIBLE
        layoutOpcionesVenta.visibility = View.VISIBLE
        layoutOpcionesVenta.translationY = layoutOpcionesVenta.height.toFloat()
        layoutOpcionesVenta.animate().translationY(0f).setDuration(250).start()
    }

    private fun cerrarModalVenta() {
        layoutOpcionesVenta.animate()
            .translationY(layoutOpcionesVenta.height.toFloat())
            .setDuration(250)
            .withEndAction {
                overlayVenta.visibility = View.GONE
                layoutOpcionesVenta.visibility = View.GONE
            }.start()
    }
}