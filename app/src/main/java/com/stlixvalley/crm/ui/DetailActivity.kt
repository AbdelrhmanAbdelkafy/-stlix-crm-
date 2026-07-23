package com.stlixvalley.crm.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stlixvalley.crm.App
import com.stlixvalley.crm.R
import com.stlixvalley.crm.data.CrmModule
import com.stlixvalley.crm.data.Modules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Record detail: all non-empty fields as label : value, using module metadata. */
class DetailActivity : AppCompatActivity() {

    private val app get() = App.instance
    private val scope = MainScope()
    private lateinit var module: CrmModule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        module = Modules.byKey(intent.getStringExtra("module") ?: "contacts")
        val id = intent.getStringExtra("id") ?: run { finish(); return }
        setContentView(R.layout.activity_detail)
        title = module.title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val container = findViewById<LinearLayout>(R.id.fields)
        val status = findViewById<TextView>(R.id.txtStatus)
        val v = app.vtiger ?: run { finish(); return }

        scope.launch {
            val res = withContext(Dispatchers.IO) {
                runCatching {
                    val rec = v.retrieve(id)
                    val labels = runCatching { fieldLabels(v.describe(module.module)) }.getOrDefault(emptyMap())
                    rec to labels
                }
            }
            res.onSuccess { (rec, labels) ->
                status.visibility = View.GONE
                addWhatsAppButton(container, rec)
                for (key in rec.keys()) {
                    val value = rec.optString(key)
                    if (value.isBlank() || key == "id") continue
                    val v2 = layoutInflater.inflate(R.layout.item_field, container, false)
                    v2.findViewById<TextView>(R.id.fieldLabel).text = labels[key] ?: key
                    v2.findViewById<TextView>(R.id.fieldValue).text = value
                    container.addView(v2)
                }
            }.onFailure { status.text = getString(R.string.load_failed, it.message ?: "") }
        }
    }

    /** Free WhatsApp via a wa.me deep-link — opens WhatsApp with a ready message
     *  to the record's mobile/phone. No paid Business API needed. */
    private fun addWhatsAppButton(container: LinearLayout, rec: JSONObject) {
        val raw = listOf("mobile", "phone", "homephone", "otherphone")
            .map { rec.optString(it) }
            .firstOrNull { it.isNotBlank() && it.count { c -> c.isDigit() } >= 8 } ?: return
        val number = normalizeNumber(raw)
        if (number.length < 10) return

        val btn = com.google.android.material.button.MaterialButton(this).apply {
            text = getString(R.string.whatsapp)
            setBackgroundColor(0xFF25D366.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.bottomMargin = (12 * resources.displayMetrics.density).toInt()
            layoutParams = lp
            setOnClickListener {
                val name = rec.optString("firstname").ifBlank { rec.optString("potentialname") }
                val hi = getString(R.string.whatsapp_greeting, name).trim()
                val url = "https://wa.me/$number?text=" + android.net.Uri.encode(hi)
                runCatching {
                    startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
                }.onFailure {
                    android.widget.Toast.makeText(this@DetailActivity, R.string.whatsapp_missing, android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        container.addView(btn, 0)
    }

    /** Egypt-friendly normalization: keep digits, turn a leading 0 into 20. */
    private fun normalizeNumber(raw: String): String {
        var d = raw.filter { it.isDigit() }
        if (d.startsWith("00")) d = d.drop(2)
        if (d.startsWith("0")) d = "20" + d.drop(1)
        return d
    }

    private fun fieldLabels(describe: JSONObject): Map<String, String> {
        val out = HashMap<String, String>()
        val fields = describe.optJSONArray("fields") ?: return out
        for (i in 0 until fields.length()) {
            val f = fields.getJSONObject(i)
            out[f.optString("name")] = f.optString("label", f.optString("name"))
        }
        return out
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
