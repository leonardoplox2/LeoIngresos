package com.example.myapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.DecimalFormat
import android.text.Editable
import android.text.TextWatcher

class BalanceActivity : AppCompatActivity() {

    // 🔹 VISTAS Y DATOS
    private lateinit var tvNombre: TextView
    private lateinit var imgAvatar: ImageView
    private lateinit var txtBalance: TextView
    private lateinit var txtIngresosAmount: TextView
    private lateinit var txtEgresosAmount: TextView
    private lateinit var etBuscar: EditText

    private lateinit var btnNuevaVenta: Button
    private lateinit var btnVentaLibre: Button
    private lateinit var btnNuevoGasto: Button
    private lateinit var layoutOpcionesVenta: LinearLayout
    private lateinit var overlayVenta: FrameLayout

    // 🔹 PERSISTENCIA - AHORA CON USUARIO ESPECÍFICO
    private var listaMovimientos = mutableListOf<Movimiento>()
    private val PREFS_BALANCE = "BalancePrefs"
    // 🔥 KEY_MOVIMIENTOS ahora será dinámico por usuario
    private lateinit var keyMovimientosUsuario: String

    // 🔹 FRAGMENTO
    private val movimientoFragment = MovimientoFragment()

    // ➡️ LAUNCHER: Manejo del resultado de VentaLibreActivity (Ingreso)
    private val ventaLibreResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val valor = data?.getDoubleExtra("valor", 0.0) ?: 0.0
            val concepto = data?.getStringExtra("concepto") ?: "Venta Libre"
            val fecha = data?.getStringExtra("fecha") ?: ""
            val metodoPago = data?.getStringExtra("metodoPago") ?: "Efectivo"

            if (valor > 0.0) {
                val nuevoIngreso = Movimiento("Ingreso", concepto, valor, fecha, metodoPago)
                listaMovimientos.add(nuevoIngreso)
                guardarMovimientos()
                actualizarUIBalance()
                Toast.makeText(this, "✅ Venta de $concepto registrada por S/ ${DecimalFormat("#,##0.00").format(valor)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ➡️ LAUNCHER: Manejo del resultado de NuevoGastoActivity (Egreso)
    private val nuevoGastoResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val monto = data?.getDoubleExtra("monto", 0.0) ?: 0.0
            val concepto = data?.getStringExtra("concepto") ?: "Gasto"
            val fecha = data?.getStringExtra("fecha") ?: ""
            val metodoPago = data?.getStringExtra("metodoPago") ?: "Efectivo"

            if (monto > 0.0) {
                val nuevoEgreso = Movimiento("Egreso", concepto, monto, fecha, metodoPago)
                listaMovimientos.add(nuevoEgreso)
                guardarMovimientos()
                actualizarUIBalance()
                Toast.makeText(this, "🛑 Gasto de $concepto registrado por S/ ${DecimalFormat("#,##0.00").format(monto)}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 🔥 ACTUALIZAR KEY DEL USUARIO EN CADA RESUME
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val usuario = prefs.getString("usuario", "default_user") ?: "default_user"
        keyMovimientosUsuario = "movimientos_$usuario"

        // 🔥 ACTUALIZAR NOMBRE EN EL HEADER
        val nombre = prefs.getString("nombre", "Usuario")
        val apellido = prefs.getString("apellido", "")
        tvNombre.text = "$nombre $apellido"

        cargarMovimientos()
        actualizarUIBalance()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_balance)

        // 🔥 OBTENER USUARIO ACTUAL Y CREAR KEY ÚNICA
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val usuario = prefs.getString("usuario", "default_user") ?: "default_user"

        // 🔥 Crear key única por usuario
        keyMovimientosUsuario = "movimientos_$usuario"

        // 1. INICIALIZACIÓN DE VISTAS DE BALANCE Y DATOS
        txtBalance = findViewById(R.id.txtBalance)
        txtIngresosAmount = findViewById(R.id.txtIngresosAmount)
        txtEgresosAmount = findViewById(R.id.txtEgresosAmount)

        // 2. INICIALIZACIÓN Y LÓGICA DEL HEADER (Nombre y Perfil)
        val nombre = prefs.getString("nombre", "Usuario")
        val apellido = prefs.getString("apellido", "")

        tvNombre = findViewById(R.id.tvBienvenido)
        tvNombre.text = "$nombre $apellido"

        imgAvatar = findViewById(R.id.imgAvatar)
        imgAvatar.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        // 3. Carga de datos y Fragmento
        cargarMovimientos()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, movimientoFragment)
                .commit()
        }

        // 4. ACTUALIZACIÓN INICIAL (CardView y Fragmento)
        actualizarUIBalance()

        // 5. INICIALIZACIÓN Y LÓGICA DE BÚSQUEDA
        etBuscar = findViewById(R.id.etBuscar)

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val textoBusqueda = s.toString()
                if (movimientoFragment.isAdded) {
                    movimientoFragment.filtrarPorTexto(textoBusqueda)
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // 6. INICIALIZACIÓN DEL MENÚ DE VENTA/GASTO
        btnNuevaVenta = findViewById(R.id.btnNuevaVenta)
        overlayVenta = findViewById(R.id.overlayVenta)
        layoutOpcionesVenta = findViewById(R.id.layoutOpcionesVenta)
        btnVentaLibre = findViewById(R.id.btnVentaLibre)
        btnNuevoGasto = findViewById(R.id.btnNuevoGasto)
        val btnCerrarOverlay = findViewById<ImageView>(R.id.btnCerrarVenta)

        btnNuevaVenta.setOnClickListener {
            overlayVenta.visibility = View.VISIBLE
            layoutOpcionesVenta.visibility = View.VISIBLE
            layoutOpcionesVenta.translationY = layoutOpcionesVenta.height.toFloat()
            layoutOpcionesVenta.animate()
                .translationY(0f)
                .setDuration(250)
                .start()
        }
        btnCerrarOverlay.setOnClickListener { cerrarModalVenta() }

        btnVentaLibre.setOnClickListener {
            cerrarModalVenta()
            val intent = Intent(this, VentaLibreActivity::class.java)
            ventaLibreResultLauncher.launch(intent)
        }

        btnNuevoGasto.setOnClickListener {
            cerrarModalVenta()
            val intent = Intent(this, NuevoGastoActivity::class.java)
            nuevoGastoResultLauncher.launch(intent)
        }

        // 7. LÓGICA DEL FOOTER
        val btnHome = findViewById<LinearLayout>(R.id.btnHome)
        val btnBalance = findViewById<LinearLayout>(R.id.btnBalance)
        val btnInventario = findViewById<LinearLayout>(R.id.btnInventario)

        btnHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
        }

        btnBalance.setOnClickListener {
            Toast.makeText(this, "Ya estás en Balance", Toast.LENGTH_SHORT).show()
        }

        btnInventario.setOnClickListener {
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

    private fun actualizarUIBalance() {
        val ingresosTotales = listaMovimientos.filter { it.tipo == "Ingreso" }.sumOf { it.monto }
        val egresosTotales = listaMovimientos.filter { it.tipo == "Egreso" }.sumOf { it.monto }
        val balance = ingresosTotales - egresosTotales

        val df = DecimalFormat("S/ #,##0.00")

        txtBalance.text = df.format(balance)
        txtIngresosAmount.text = df.format(ingresosTotales)
        txtEgresosAmount.text = df.format(egresosTotales)

        actualizarFragmento(listaMovimientos)
    }

    private fun actualizarFragmento(lista: List<Movimiento>) {
        if (movimientoFragment.isAdded) {
            movimientoFragment.actualizarMovimientos(lista)
        }
    }

    private fun cerrarModalVenta() {
        layoutOpcionesVenta.animate()
            .translationY(layoutOpcionesVenta.height.toFloat())
            .setDuration(250)
            .withEndAction {
                overlayVenta.visibility = View.GONE
                layoutOpcionesVenta.visibility = View.GONE
            }
            .start()
    }
}