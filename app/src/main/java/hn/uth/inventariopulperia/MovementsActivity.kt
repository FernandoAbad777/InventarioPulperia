package hn.uth.inventariopulperia

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MovementsActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var spProduct: Spinner
    private lateinit var etQty: EditText
    private lateinit var rbIn: RadioButton
    private lateinit var listMovements: ListView
    private lateinit var tvEmpty: TextView
    private var products = listOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movements)
        title = "Movimientos"
        db = DBHelper(this)

        spProduct = findViewById(R.id.spProduct)
        etQty = findViewById(R.id.etQty)
        rbIn = findViewById(R.id.rbIn)
        listMovements = findViewById(R.id.listMovements)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<Button>(R.id.btnRegister).setOnClickListener { register() }
        loadProducts()
        loadMovements()
    }

    private fun loadProducts() {
        products = db.getProducts()
        val names = products.map { "${it.name} (stock: ${it.stock})" }
        spProduct.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        tvEmpty.text = if (products.isEmpty()) "Primero registre productos en la pantalla Productos." else ""
    }

    private fun loadMovements() {
        val rows = db.getMovements().map {
            val sign = if (it.type == "ENTRADA") "+" else "-"
            "${it.date}\n${it.type}: ${it.productName}  ($sign${it.qty})"
        }
        listMovements.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }

    private fun register() {
        if (products.isEmpty()) { toast("No hay productos"); return }
        val qty = etQty.text.toString().toIntOrNull()
        if (qty == null || qty <= 0) { toast("Cantidad inválida"); return }

        val product = products[spProduct.selectedItemPosition]
        val type = if (rbIn.isChecked) "ENTRADA" else "SALIDA"

        val error = db.registerMovement(product.id, qty, type)
        if (error != null) { toast(error); return }

        toast("Movimiento registrado")
        etQty.text.clear()

        // Alerta de stock mínimo
        val updated = db.getProduct(product.id)
        if (updated != null && updated.isLow) {
            AlertDialog.Builder(this)
                .setTitle("⚠ Stock mínimo")
                .setMessage("\"${updated.name}\" tiene stock bajo: ${updated.stock} (mínimo ${updated.minStock})")
                .setPositiveButton("OK", null)
                .show()
        }

        val selected = spProduct.selectedItemPosition
        loadProducts()
        spProduct.setSelection(selected)
        loadMovements()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}