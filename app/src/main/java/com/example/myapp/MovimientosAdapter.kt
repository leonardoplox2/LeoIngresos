package com.example.myapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

class MovimientosAdapter(private val items: List<Movimiento>) :
    RecyclerView.Adapter<MovimientosAdapter.MovimientoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovimientoViewHolder {
        // Usamos el layout 'item_movimiento'
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_movimiento, parent, false)
        return MovimientoViewHolder(view)
    }

    override fun onBindViewHolder(holder: MovimientoViewHolder, position: Int) {
        // Llama a la función bind del ViewHolder para configurar las vistas
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class MovimientoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 🚨 IDs ACTUALIZADOS (iguales a tu item_movimiento.xml)
        private val tvConcepto: TextView = itemView.findViewById(R.id.tvConceptoItem)
        private val tvMonto: TextView = itemView.findViewById(R.id.tvMontoItem)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFechaItem)
        private val tvMetodoPago: TextView = itemView.findViewById(R.id.tvMetodoPagoItem)

        fun bind(movimiento: Movimiento) {
            val df = DecimalFormat("#,##0.00")

            // 🔹 Asignar datos básicos
            tvConcepto.text = movimiento.concepto
            tvFecha.text = movimiento.fecha
            tvMetodoPago.text = movimiento.metodoPago

            // 🔹 Lógica de Egreso vs. Ingreso
            if (movimiento.tipo == "Ingreso") {
                tvMonto.text = "+ S/ ${df.format(movimiento.monto)}"
                // Usamos el color verde que especificaste
                tvMonto.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                tvMonto.text = "- S/ ${df.format(movimiento.monto)}"
                // Usamos el color rojo que especificaste
                tvMonto.setTextColor(Color.parseColor("#F44336"))
            }
        }
    }
}