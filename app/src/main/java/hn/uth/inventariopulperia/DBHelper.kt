package hn.uth.inventariopulperia

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Product(
    val id: Long,
    val name: String,
    val price: Double,
    val stock: Int,
    val barcode: String,
    val minStock: Int
) {
    val value: Double get() = price * stock
    val isLow: Boolean get() = stock <= minStock
}

data class Movement(
    val id: Long,
    val productId: Long,
    val productName: String,
    val qty: Int,
    val type: String,
    val date: String
)

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "pulperia.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE products (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price REAL NOT NULL,
                stock INTEGER NOT NULL,
                barcode TEXT,
                min_stock INTEGER NOT NULL DEFAULT 5
            )"""
        )
        db.execSQL(
            """CREATE TABLE movements (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                product_id INTEGER NOT NULL,
                qty INTEGER NOT NULL,
                type TEXT NOT NULL,
                date TEXT NOT NULL,
                FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS movements")
        db.execSQL("DROP TABLE IF EXISTS products")
        onCreate(db)
    }

    // ---------- CRUD productos ----------

    fun insertProduct(name: String, price: Double, stock: Int, barcode: String, minStock: Int): Long {
        val cv = ContentValues().apply {
            put("name", name)
            put("price", price)
            put("stock", stock)
            put("barcode", barcode)
            put("min_stock", minStock)
        }
        return writableDatabase.insert("products", null, cv)
    }

    fun updateProduct(id: Long, name: String, price: Double, stock: Int, barcode: String, minStock: Int) {
        val cv = ContentValues().apply {
            put("name", name)
            put("price", price)
            put("stock", stock)
            put("barcode", barcode)
            put("min_stock", minStock)
        }
        writableDatabase.update("products", cv, "id=?", arrayOf(id.toString()))
    }

    fun deleteProduct(id: Long) {
        writableDatabase.delete("products", "id=?", arrayOf(id.toString()))
    }

    fun getProducts(): List<Product> {
        val list = mutableListOf<Product>()
        val c = readableDatabase.rawQuery(
            "SELECT id, name, price, stock, barcode, min_stock FROM products ORDER BY name", null
        )
        while (c.moveToNext()) {
            list.add(
                Product(
                    c.getLong(0), c.getString(1), c.getDouble(2), c.getInt(3),
                    c.getString(4) ?: "", c.getInt(5)
                )
            )
        }
        c.close()
        return list
    }

    fun getProduct(id: Long): Product? = getProducts().firstOrNull { it.id == id }

    // ---------- Movimientos ----------

    /** Devuelve null si todo salió bien, o un mensaje de error. */
    fun registerMovement(productId: Long, qty: Int, type: String): String? {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val c = db.rawQuery("SELECT stock FROM products WHERE id=?", arrayOf(productId.toString()))
            var stock = -1
            if (c.moveToFirst()) stock = c.getInt(0)
            c.close()
            if (stock < 0) return "Producto no encontrado"

            val newStock = if (type == "ENTRADA") stock + qty else stock - qty
            if (newStock < 0) return "Stock insuficiente (disponible: $stock)"

            val cvP = ContentValues().apply { put("stock", newStock) }
            db.update("products", cvP, "id=?", arrayOf(productId.toString()))

            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            val cvM = ContentValues().apply {
                put("product_id", productId)
                put("qty", qty)
                put("type", type)
                put("date", date)
            }
            db.insert("movements", null, cvM)
            db.setTransactionSuccessful()
            return null
        } finally {
            db.endTransaction()
        }
    }

    fun getMovements(): List<Movement> {
        val list = mutableListOf<Movement>()
        val c = readableDatabase.rawQuery(
            """SELECT m.id, m.product_id, p.name, m.qty, m.type, m.date
               FROM movements m JOIN products p ON p.id = m.product_id
               ORDER BY m.id DESC""", null
        )
        while (c.moveToNext()) {
            list.add(Movement(c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3), c.getString(4), c.getString(5)))
        }
        c.close()
        return list
    }

    // ---------- Resumen ----------

    fun totalInventoryValue(): Double {
        val c = readableDatabase.rawQuery("SELECT COALESCE(SUM(price * stock), 0) FROM products", null)
        var total = 0.0
        if (c.moveToFirst()) total = c.getDouble(0)
        c.close()
        return total
    }

    fun top5ByValue(): List<Product> =
        getProducts().sortedByDescending { it.value }.take(5)

    fun lowStockProducts(): List<Product> =
        getProducts().filter { it.isLow }
}