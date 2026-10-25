package hn.uth.inventariopulperia

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summary)
        title = "Resumen"
    }

    override fun onResume() {
        super.onResume()
        val db = DBHelper(this)

        findViewById<TextView>(R.id.tvTotal).text =
            "L. %.2f".format(db.totalInventoryValue())

        val top = db.top5ByValue()
        findViewById<TextView>(R.id.tvTop).text =
            if (top.isEmpty()) "Sin productos registrados"
            else top.mapIndexed { i, p ->
                "${i + 1}. ${p.name}\n    L. %.2f  (L. %.2f x ${p.stock})".format(p.value, p.price)
            }.joinToString("\n\n")

        val low = db.lowStockProducts()
        findViewById<TextView>(R.id.tvLow).text =
            if (low.isEmpty()) "Ningún producto en stock mínimo ✔"
            else low.joinToString("\n") { "⚠ ${it.name}: ${it.stock} (mín ${it.minStock})" }
    }
}