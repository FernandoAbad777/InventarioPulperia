package hn.uth.inventariopulperia

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class ProductsActivity : AppCompatActivity() {

    private lateinit var db: DBHelper
    private lateinit var list: ListView
    private lateinit var etName: EditText
    private lateinit var etPrice: EditText
    private lateinit var etStock: EditText
    private lateinit var etMin: EditText
    private lateinit var etBarcode: EditText
    private lateinit var btnSave: Button
    private var editingId: Long? = null
    private var products = listOf<Product>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)
        title = "Productos"
        db = DBHelper(this)

        list = findViewById(R.id.listProducts)
        val header = layoutInflater.inflate(R.layout.header_product_form, list, false)
        list.addHeaderView(header, null, false)

        etName = header.findViewById(R.id.etName)
        etPrice = header.findViewById(R.id.etPrice)
        etStock = header.findViewById(R.id.etStock)
        etMin = header.findViewById(R.id.etMin)
        etBarcode = header.findViewById(R.id.etBarcode)
        btnSave = header.findViewById(R.id.btnSave)
        val btnClear = header.findViewById<Button>(R.id.btnClear)

        btnSave.setOnClickListener { saveProduct() }
        btnClear.setOnClickListener { clearForm() }

        // Toque corto: editar
        list.setOnItemClickListener { _, _, position, _ ->
            val index = position - list.headerViewsCount
            if (index in products.indices) loadForEdit(products[index])
        }
        // Toque largo: eliminar
        list.setOnItemLongClickListener { _, _, position, _ ->
            val index = position - list.headerViewsCount
            if (index in products.indices) confirmDelete(products[index])
            true
        }

        refresh()
    }

    private fun saveProduct() {
        val name = etName.text.toString().trim()
        val price = etPrice.text.toString().toDoubleOrNull()
        val stock = etStock.text.toString().toIntOrNull()
        val min = etMin.text.toString().toIntOrNull() ?: 5
        val barcode = etBarcode.text.toString().trim()

        if (name.isEmpty()) { toast("Ingrese el nombre"); return }
        if (price == null || price < 0) { toast("Precio inválido"); return }
        if (stock == null || stock < 0) { toast("Stock inválido"); return }

        val id = editingId
        if (id == null) {
            db.insertProduct(name, price, stock, barcode, min)
            toast("Producto creado")
        } else {
            db.updateProduct(id, name, price, stock, barcode, min)
            toast("Producto actualizado")
        }

        if (stock <= min) {
            AlertDialog.Builder(this)
                .setTitle("⚠ Stock mínimo")
                .setMessage("\"$name\" tiene stock bajo ($stock). Mínimo: $min")
                .setPositiveButton("OK", null)
                .show()
        }
        clearForm()
        refresh()
    }

    private fun loadForEdit(p: Product) {
        editingId = p.id
        etName.setText(p.name)
        etPrice.setText(p.price.toString())
        etStock.setText(p.stock.toString())
        etMin.setText(p.minStock.toString())
        etBarcode.setText(p.barcode)
        btnSave.text = "Actualizar"
    }

    private fun confirmDelete(p: Product) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar producto")
            .setMessage("¿Eliminar \"${p.name}\" y sus movimientos?")
            .setPositiveButton("Eliminar") { _, _ ->
                db.deleteProduct(p.id)
                if (editingId == p.id) clearForm()
                refresh()
                toast("Producto eliminado")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun clearForm() {
        editingId = null
        etName.text.clear()
        etPrice.text.clear()
        etStock.text.clear()
        etMin.setText("5")
        etBarcode.text.clear()
        btnSave.text = "Guardar"
    }

    private fun refresh() {
        products = db.getProducts()
        val rows = products.map {
            val alert = if (it.isLow) "  ⚠ STOCK BAJO" else ""
            val code = if (it.barcode.isNotEmpty()) "\nCódigo: ${it.barcode}" else ""
            "${it.name}\nL. %.2f  |  Stock: ${it.stock} (mín ${it.minStock})$alert$code".format(it.price)
        }
        list.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, rows)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}