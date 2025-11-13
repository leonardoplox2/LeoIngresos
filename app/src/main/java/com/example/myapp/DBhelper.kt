package com.example.tusistema

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBhelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "usuarios.db"
        private const val DATABASE_VERSION = 2   // 🔹 Subido para que actualice la tabla
        private const val TABLE_NAME = "usuarios"

        private const val COL_ID = "id"
        private const val COL_NOMBRE_APELLIDO = "nombre_apellido"
        private const val COL_USUARIO = "usuario"
        private const val COL_PASSWORD = "password"
        private const val COL_NUMERO = "numero"
        private const val COL_DNI = "dni"
        private const val COL_CORREO = "correo"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NOMBRE_APELLIDO TEXT,
                $COL_USUARIO TEXT UNIQUE,
                $COL_PASSWORD TEXT,
                $COL_NUMERO TEXT,
                $COL_DNI TEXT,
                $COL_CORREO TEXT
            )
        """
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // 🔹 Insertar usuario nuevo (correo y dni vacíos al inicio)
    fun insertarUsuario(nombreApellido: String, usuario: String, password: String, numero: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOMBRE_APELLIDO, nombreApellido)
            put(COL_USUARIO, usuario)
            put(COL_PASSWORD, password)
            put(COL_NUMERO, numero)
            put(COL_DNI, "")    // vacíos al registrar
            put(COL_CORREO, "")
        }
        val result = db.insert(TABLE_NAME, null, values)
        db.close()
        return result != -1L
    }

    // 🔹 Verificar si usuario y contraseña son correctos
    fun verificarUsuario(usuario: String, password: String): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COL_USUARIO = ? AND $COL_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(usuario, password))
        val existe = cursor.count > 0
        cursor.close()
        db.close()
        return existe
    }

    // 🔹 Obtener nombre completo
    fun obtenerNombreCompleto(usuario: String): String? {
        val db = readableDatabase
        val query = "SELECT $COL_NOMBRE_APELLIDO FROM $TABLE_NAME WHERE $COL_USUARIO = ?"
        val cursor = db.rawQuery(query, arrayOf(usuario))
        var nombreApellido: String? = null
        if (cursor.moveToFirst()) {
            nombreApellido = cursor.getString(0)
        }
        cursor.close()
        db.close()
        return nombreApellido
    }

    // 🔹 Obtener todos los datos de un usuario
    fun obtenerUsuario(usuario: String): Map<String, String>? {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_NAME WHERE $COL_USUARIO = ?"
        val cursor = db.rawQuery(query, arrayOf(usuario))
        var datos: Map<String, String>? = null
        if (cursor.moveToFirst()) {
            datos = mapOf(
                "nombre_apellido" to cursor.getString(cursor.getColumnIndexOrThrow(COL_NOMBRE_APELLIDO)),
                "usuario" to cursor.getString(cursor.getColumnIndexOrThrow(COL_USUARIO)),
                "numero" to cursor.getString(cursor.getColumnIndexOrThrow(COL_NUMERO)),
                "dni" to cursor.getString(cursor.getColumnIndexOrThrow(COL_DNI)),
                "correo" to cursor.getString(cursor.getColumnIndexOrThrow(COL_CORREO))
            )
        }
        cursor.close()
        db.close()
        return datos
    }

    // 🔹 Actualizar datos de un usuario
    fun actualizarUsuario(usuario: String, nombreApellido: String, numero: String, dni: String, correo: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_NOMBRE_APELLIDO, nombreApellido)
            put(COL_NUMERO, numero)
            put(COL_DNI, dni)
            put(COL_CORREO, correo)
        }
        val result = db.update(TABLE_NAME, values, "$COL_USUARIO = ?", arrayOf(usuario))
        db.close()
        return result > 0
    }
}
