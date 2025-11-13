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
import com.google.gson.Gson // 🚨 IMPORTANTE: Necesario para guardar/cargar JSON
import com.google.gson.reflect.TypeToken // 🚨 IMPORTANTE: Necesario para Gson

class MainActivity : AppCompatActivity() {

    // 🔹 VISTAS DEL HEADER
    private lateinit var tvBienvenido: TextView
    private lateinit var imgAvatar: ImageView

    // 🚨 VISTAS NECESARIAS PARA LAS TARJETAS Y EL MODAL 🚨
    private lateinit var overlayVenta: FrameLayout
    private lateinit var layoutOpcionesVenta: LinearLayout
    private lateinit var btnVentaLibre: Button
    private lateinit var btnVentaProductos: Button
    private lateinit var cardVenta: CardView
    private lateinit var cardGastos: CardView
    private lateinit var cardInventario: CardView

    // 🔹 PERSISTENCIA NECESARIA PARA GUARDAR MOVIMIENTOS
    private var listaMovimientos = mutableListOf<Movimiento>()
    private val PREFS_BALANCE = "BalancePrefs"
    private val KEY_MOVIMIENTOS = "movimientos"


    // ➡️ LAUNCHER: Para manejar el resultado del registro de movimiento (Ingreso/Egreso)
    private val registroMovimientoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data

            // 1. Cargar la lista existente antes de añadir el nuevo movimiento
            cargarMovimientos()

            // 2. Intentamos leer la data como un INGRESO (Venta Libre)
            val valor = data?.getDoubleExtra("valor", 0.0) ?: 0.0
            val conceptoVenta = data?.getStringExtra("concepto")

            // 3. Intentamos leer la data como un EGRESO (Nuevo Gasto)
            val montoGasto = data?.getDoubleExtra("monto", 0.0) ?: 0.0
            val conceptoGasto = data?.getStringExtra("concepto")

            // Datos comunes
            val fecha = data?.getStringExtra("fecha") ?: ""
            val metodoPago = data?.getStringExtra("metodoPago") ?: "Efectivo"

            // --- LÓGICA DE GUARDADO ---
            if (valor > 0.0 && conceptoVenta != null) {
                // Es un INGRESO (Venta Libre)
                val nuevoIngreso = Movimiento("Ingreso", conceptoVenta, valor, fecha, metodoPago)
                listaMovimientos.add(nuevoIngreso)
                guardarMovimientos()
                Toast.makeText(this, "✅ Venta registrada y guardada.", Toast.LENGTH_SHORT).show()

            } else if (montoGasto > 0.0 && conceptoGasto != null) {
                // Es un EGRESO (Nuevo Gasto)
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

        // 1. INICIALIZACIÓN DE VISTAS
        tvBienvenido = findViewById(R.id.tvBienvenido)
        imgAvatar = findViewById(R.id.imgAvatar)
        cardVenta = findViewById(R.id.cardVenta)
        cardGastos = findViewById(R.id.cardGastos)
        cardInventario = findViewById(R.id.cardInventario)
        overlayVenta = findViewById(R.id.overlayVenta)
        layoutOpcionesVenta = findViewById(R.id.layoutOpcionesVenta)
        btnVentaLibre = findViewById(R.id.btnVentaLibre)
        btnVentaProductos = findViewById(R.id.btnVentaProductos)
        val btnCerrarOverlay = findViewById<ImageView>(R.id.btnCerrarVenta)
        val btnHome = findViewById<LinearLayout>(R.id.btnHome)
        val btnBalance = findViewById<LinearLayout>(R.id.btnBalance)
        val btnInventarioFooter = findViewById<LinearLayout>(R.id.btnInventario)

        // 2. LÓGICA DE HEADER
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val nombre = prefs.getString("nombre", "Usuario")
        val apellido = prefs.getString("apellido", "")
        tvBienvenido.text = "Bienvenido $nombre $apellido 👋"

        imgAvatar.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        // 3. LÓGICA DE DASHBOARD Y MODAL

        // 🟢 cardVenta: Lanza el modal
        cardVenta.setOnClickListener { mostrarModalVenta() }

        // 🔴 cardGastos: Lanza directamente la Activity de Nuevo Gasto
        cardGastos.setOnClickListener {
            registroMovimientoLauncher.launch(Intent(this, NuevoGastoActivity::class.java))
        }

        // cardInventario: Navega a la pantalla de inventario
        cardInventario.setOnClickListener {
            val intent = Intent(this, InventarioActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        // LÓGICA DEL MODAL
        btnCerrarOverlay.setOnClickListener { cerrarModalVenta() }

        btnVentaLibre.setOnClickListener {
            cerrarModalVenta()
            registroMovimientoLauncher.launch(Intent(this, VentaLibreActivity::class.java))
        }

        btnVentaProductos.setOnClickListener {
            cerrarModalVenta()
            // Aquí iría el código para iniciar VentaProductosActivity
        }

        // 4. LÓGICA DEL FOOTER
        btnHome.setOnClickListener { /* Ya estamos en Home */ }

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
        val json = prefs.getString(KEY_MOVIMIENTOS, null)

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
        editor.putString(KEY_MOVIMIENTOS, json)
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