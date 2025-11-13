package com.example.myapp

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class NuevoGastoActivity : AppCompatActivity() {

    private lateinit var etFechaGasto: EditText
    private lateinit var etMontoGasto: EditText
    private lateinit var etConceptoGasto: EditText
    private lateinit var spCategoriaGasto: Spinner
    private lateinit var rgMetodoPagoGasto: RadioGroup
    private lateinit var btnRegistrarGasto: Button
    private lateinit var btnVolver: ImageButton // Cambiado a ImageButton para coincidir con XML

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nuevo_gasto)

        // 1. Inicialización de vistas
        etFechaGasto = findViewById(R.id.etFechaGasto)
        etMontoGasto = findViewById(R.id.etMontoGasto)
        etConceptoGasto = findViewById(R.id.etConceptoGasto)
        spCategoriaGasto = findViewById(R.id.spCategoriaGasto)
        rgMetodoPagoGasto = findViewById(R.id.rgMetodoPagoGasto)
        btnRegistrarGasto = findViewById(R.id.btnRegistrarGasto)
        btnVolver = findViewById(R.id.btnVolver) // Ya es ImageButton

        // 2. Configurar Spinner de Categorías (Ejemplo de gastos)
        val categorias = arrayOf("Alquiler", "Servicios", "Mercadería/Inventario", "Salarios", "Transporte", "Impuestos", "Otro")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categorias)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategoriaGasto.adapter = adapter

        // 3. Lógica para seleccionar fecha
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etFechaGasto.setText(dateFormat.format(calendar.time)) // Fecha actual por defecto

        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            calendar.set(year, monthOfYear, dayOfMonth)
            etFechaGasto.setText(dateFormat.format(calendar.time))
        }

        etFechaGasto.setOnClickListener {
            DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // 4. Eventos de click
        btnVolver.setOnClickListener {
            finish()
        }

        btnRegistrarGasto.setOnClickListener {
            registrarGasto()
        }
    }

    private fun registrarGasto() {
        val montoStr = etMontoGasto.text.toString().trim()
        val concepto = etConceptoGasto.text.toString().trim()
        val fecha = etFechaGasto.text.toString()
        val categoria = spCategoriaGasto.selectedItem.toString()

        // 1. Validación de campos
        if (montoStr.isEmpty() || concepto.isEmpty()) {
            Toast.makeText(this, "Completa el monto y la descripción del gasto.", Toast.LENGTH_SHORT).show()
            return
        }

        val monto = montoStr.toDoubleOrNull()
        if (monto == null || monto <= 0) {
            Toast.makeText(this, "Ingresa un monto válido mayor a cero.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. Obtener Método de Pago
        val selectedId = rgMetodoPagoGasto.checkedRadioButtonId
        // Busca el RadioButton seleccionado y obtiene su texto. Si no hay selección, usa "Efectivo".
        val metodoPago = findViewById<RadioButton>(selectedId)?.text.toString() ?: "Efectivo"

        SonidoManager.reproducirSonidoGasto(this)

        // 3. Devolver resultado a BalanceActivity
        val resultIntent = Intent()
        resultIntent.putExtra("monto", monto)
        resultIntent.putExtra("concepto", concepto)
        resultIntent.putExtra("fecha", fecha)
        resultIntent.putExtra("metodoPago", metodoPago)
        resultIntent.putExtra("categoria", categoria) // La categoría es opcional para el objeto Movimiento

        setResult(Activity.RESULT_OK, resultIntent)

        Toast.makeText(this, "🛑 Gasto registrado correctamente", Toast.LENGTH_SHORT).show()

        finish()
    }
    override fun onDestroy() {
        super.onDestroy()
        // Liberar recursos de audio
        SonidoManager.liberarRecursos()
    }
}