    package com.example.myapp

    import android.app.DatePickerDialog
    import android.content.Intent
    import android.os.Bundle
    import android.text.Editable
    import android.text.TextWatcher
    import android.widget.*
    import androidx.appcompat.app.AppCompatActivity
    import java.util.*

    class VentaLibreActivity : AppCompatActivity() {

        private lateinit var etFechaVenta: EditText
        private lateinit var etValor: EditText
        private lateinit var etDescuento: EditText
        private lateinit var txtTotalConDescuento: TextView
        private lateinit var rgMetodoPago: RadioGroup
        private lateinit var etConcepto: EditText
        private lateinit var btnCrearVenta: Button
        private lateinit var btnVolver: ImageButton
        private lateinit var txtTotalPreview: TextView

        private var totalFinal = 0.0
        private var valorBase = 0.0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_venta_libre)

            // Inicialización de vistas... (tu código existente)
            etFechaVenta = findViewById(R.id.etFechaVenta)
            etValor = findViewById(R.id.etValor)
            etDescuento = findViewById(R.id.etDescuento)
            txtTotalConDescuento = findViewById(R.id.txtTotalConDescuento)
            rgMetodoPago = findViewById(R.id.rgMetodoPago)
            etConcepto = findViewById(R.id.etConcepto)
            btnCrearVenta = findViewById(R.id.btnCrearVenta)
            btnVolver = findViewById(R.id.btnVolver)
            txtTotalPreview = findViewById(R.id.txtTotalPreview)

            // DatePicker
            etFechaVenta.setOnClickListener {
                val c = Calendar.getInstance()
                val dp = DatePickerDialog(this, { _, year, month, day ->
                    val fecha = "%02d/%02d/%04d".format(day, month + 1, year)
                    etFechaVenta.setText(fecha)
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                dp.show()
            }

            // TextWatcher para recalcular total en tiempo real
            val watcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) = calcularTotalConDescuento()
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            }
            etValor.addTextChangedListener(watcher)
            etDescuento.addTextChangedListener(watcher)

            // Volver
            btnVolver.setOnClickListener { finish() }

            // Crear venta
            btnCrearVenta.setOnClickListener {
                if (!validarCampos()) return@setOnClickListener

                // El diálogo de éxito se encargará de devolver los datos y cerrar la Activity
                mostrarDialogoExito()
            }
            calcularTotalConDescuento() // Llamada inicial para asegurar que txtTotalConDescuento muestre 0.00
        }

        private fun calcularTotalConDescuento() {
            val valor = etValor.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0
            val descuento = etDescuento.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0

            // si descuento < 1 lo tratamos como proporción (0.1 = 10%)
            totalFinal = if (descuento > 0.0) {
                if (descuento < 1.0) {
                    valor - (valor * descuento)
                } else {
                    valor - descuento
                }
            } else {
                valor
            }
            if (totalFinal < 0) totalFinal = 0.0

            txtTotalConDescuento.text = "S/ %.2f".format(totalFinal)
            txtTotalPreview.text = "S/ %.2f".format(totalFinal)
            valorBase = valor
        }

        private fun validarCampos(): Boolean {
            // ... (Tu código de validación)
            if (etFechaVenta.text.isEmpty()) {
                Toast.makeText(this, "Selecciona la fecha", Toast.LENGTH_SHORT).show()
                return false
            }
            if (etValor.text.isEmpty() || (etValor.text.toString().toDoubleOrNull() ?: 0.0) <= 0.0) {
                Toast.makeText(this, "Ingresa un valor válido", Toast.LENGTH_SHORT).show()
                return false
            }
            if (rgMetodoPago.checkedRadioButtonId == -1) {
                Toast.makeText(this, "Selecciona un método de pago", Toast.LENGTH_SHORT).show()
                return false
            }
            if (etConcepto.text.isEmpty()) {
                Toast.makeText(this, "Escribe un concepto para la venta", Toast.LENGTH_SHORT).show()
                return false
            }
            return true
        }

        private fun mostrarDialogoExito() {

            SonidoManager.reproducirSonidoVenta(this)

            val builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle("✅ Venta creada")
            builder.setMessage("Se registró en tu balance con el valor ${"S/ %.2f".format(totalFinal)}")

            builder.setPositiveButton("Aceptar") { dialog, _ ->

                val metodoPagoId = rgMetodoPago.checkedRadioButtonId
                val metodoPago = if (metodoPagoId != -1)
                    findViewById<RadioButton>(metodoPagoId).text.toString()
                else "N/A"

                val concepto = etConcepto.text.toString()
                val fecha = etFechaVenta.text.toString()

                // ➡️ 🔑 ESTE ES EL ÚNICO BLOQUE DONDE SE DEVUELVE EL RESULTADO Y SE CIERRA LA ACTIVITY
                val intent = Intent()
                intent.putExtra("valor", totalFinal)
                intent.putExtra("concepto", concepto)
                intent.putExtra("fecha", fecha)
                intent.putExtra("metodoPago", metodoPago)

                // 🚨 Marcar el resultado como OK
                setResult(RESULT_OK, intent)

                dialog.dismiss()
                // 🚨 Cerrar VentaLibreActivity para volver a BalanceActivity
                finish()
            }
            builder.show()
        }
        override fun onDestroy() {
            super.onDestroy()
            // Liberar recursos de audio
            SonidoManager.liberarRecursos()
        }
    }