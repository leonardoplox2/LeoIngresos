package com.example.myapp

// Esta clase guarda los detalles que quieres mostrar
data class Movimiento(
    val tipo: String,       // "Ingreso" o "Egreso"
    val concepto: String,   // Nombre de la venta
    val monto: Double,      // Cantidad
    val fecha: String,      // Fecha de la venta
    val metodoPago: String  // Método de pago
)