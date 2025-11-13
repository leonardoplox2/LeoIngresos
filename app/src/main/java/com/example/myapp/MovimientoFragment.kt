package com.example.myapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color

class MovimientoFragment : Fragment() {

    private lateinit var tvIngresosFrag: TextView
    private lateinit var tvEgresosFrag: TextView
    private lateinit var rvMovimientos: RecyclerView

    private var todosLosMovimientos: List<Movimiento> = emptyList()
    private var filtroBusqueda: String = ""
    private var tipoFiltroActual: String = "Ingreso"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_movimiento_fragment, container, false)

        tvIngresosFrag = view.findViewById(R.id.tvIngresosFrag)
        tvEgresosFrag = view.findViewById(R.id.tvEgresosFrag)
        rvMovimientos = view.findViewById(R.id.rvMovimientos)

        rvMovimientos.layoutManager = LinearLayoutManager(context)

        tvIngresosFrag.setOnClickListener {
            tipoFiltroActual = "Ingreso"
            mostrarLista()
        }
        tvEgresosFrag.setOnClickListener {
            tipoFiltroActual = "Egreso"
            mostrarLista()
        }

        return view
    }

    fun actualizarMovimientos(nuevaLista: List<Movimiento>) {
        this.todosLosMovimientos = nuevaLista.sortedByDescending { it.fecha }
        mostrarLista()
    }

    // ➡️ MÉTODO CLAVE: Recibe el texto y aplica la regla de las 3 letras
    fun filtrarPorTexto(texto: String) {
        // 🚨 Aplicar la regla: si el texto es menor a 3 caracteres, lo reiniciamos a vacío
        if (texto.length >= 3 || texto.isBlank()) {
            this.filtroBusqueda = texto.lowercase()
        } else {
            // Si hay 1 o 2 letras, mantenemos el filtro anterior (o vacío)
            // Esto evita búsquedas excesivas con muy pocos caracteres
            this.filtroBusqueda = ""
        }
        mostrarLista()
    }

    private fun mostrarLista() {
        val colorActivo = resources.getColor(android.R.color.black)
        val colorInactivo = resources.getColor(android.R.color.darker_gray)

        // 1. Filtrar por TIPO (Ingreso/Egreso)
        var listaFiltrada = todosLosMovimientos.filter { it.tipo == tipoFiltroActual }

        // 2. Filtrar por TEXTO (SOLO si filtroBusqueda NO está vacío)
        if (filtroBusqueda.isNotBlank()) {
            listaFiltrada = listaFiltrada.filter {
                it.concepto.lowercase().contains(filtroBusqueda) ||
                        it.metodoPago.lowercase().contains(filtroBusqueda)
            }
        }

        // 3. Aplicar el Adapter
        rvMovimientos.adapter = MovimientosAdapter(listaFiltrada)

        // 4. Actualizar estilo de los botones (UI)
        tvIngresosFrag.setTextColor(if (tipoFiltroActual == "Ingreso") colorActivo else colorInactivo)
        tvEgresosFrag.setTextColor(if (tipoFiltroActual == "Egreso") colorActivo else colorInactivo)
    }
}