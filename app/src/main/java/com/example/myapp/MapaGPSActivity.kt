package com.example.myapp

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapaGPSActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var ubicacionActual: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa_gpsactivity)

        // Inicializar el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 🔙 Configurar botón volver
        findViewById<ImageButton>(R.id.btnVolver).setOnClickListener {
            finish() // Cierra la activity y vuelve a la anterior
        }

        // Obtener el fragmento del mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as? SupportMapFragment

        if (mapFragment != null) {
            mapFragment.getMapAsync(this)
        } else {
            Toast.makeText(this, "Error al cargar el mapa", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configurar el mapa
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isCompassEnabled = true

        // Verificar permisos y obtener ubicación
        if (verificarPermisos()) {
            habilitarMiUbicacion()
            obtenerUbicacionActual()
        } else {
            solicitarPermisos()
        }
    }

    private fun verificarPermisos(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisos() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun habilitarMiUbicacion() {
        try {
            if (verificarPermisos()) {
                mMap.isMyLocationEnabled = true
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun obtenerUbicacionActual() {
        try {
            if (verificarPermisos()) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            ubicacionActual = location
                            mostrarUbicacionEnMapa(location)
                        } else {
                            Toast.makeText(
                                this,
                                "No se pudo obtener la ubicación. Activa el GPS.",
                                Toast.LENGTH_LONG
                            ).show()
                            // Mostrar ubicación por defecto (Lima, Perú)
                            mostrarUbicacionPorDefecto()
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        Toast.makeText(
                            this,
                            "Error al obtener ubicación",
                            Toast.LENGTH_SHORT
                        ).show()
                        mostrarUbicacionPorDefecto()
                    }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun mostrarUbicacionEnMapa(location: Location) {
        val posicion = LatLng(location.latitude, location.longitude)

        // Limpiar marcadores anteriores
        mMap.clear()

        // Agregar marcador en la ubicación actual
        mMap.addMarker(
            MarkerOptions()
                .position(posicion)
                .title("Mi Ubicación")
                .snippet("Lat: ${String.format("%.6f", location.latitude)}, Lon: ${String.format("%.6f", location.longitude)}")
        )

        // Mover la cámara a la ubicación con zoom
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicion, 15f))

        Toast.makeText(
            this,
            "📍 Ubicación encontrada",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun mostrarUbicacionPorDefecto() {
        // Coordenadas de Lima, Perú por defecto
        val lima = LatLng(-12.046374, -77.042793)
        mMap.addMarker(
            MarkerOptions()
                .position(lima)
                .title("Lima, Perú")
                .snippet("Ubicación por defecto")
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lima, 12f))
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                habilitarMiUbicacion()
                obtenerUbicacionActual()
            } else {
                Toast.makeText(
                    this,
                    "Permiso de ubicación denegado",
                    Toast.LENGTH_LONG
                ).show()
                mostrarUbicacionPorDefecto()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
}