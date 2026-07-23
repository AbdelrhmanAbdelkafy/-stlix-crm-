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
