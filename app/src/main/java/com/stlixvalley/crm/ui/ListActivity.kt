package com.stlixvalley.crm.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
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
import org.json.JSONArray
import org.json.JSONObject

/** Generic module list: pulls the latest records and renders title + subtitle. */
class ListActivity : AppCompatActivity() {

    private val app get() = App.instance
    private val scope = MainScope()
    private lateinit var module: CrmModule

    private lateinit var container: LinearLayout
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        module = Modules.byKey(intent.getStringExtra("module") ?: "contacts")
        setContentView(R.layout.activity_list)
        title = module.title
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        container = findViewById(R.id.recordList)
        status = findViewById(R.id.txtStatus)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val v = app.vtiger ?: run { finish(); return }
        status.text = getString(R.string.loading)
        status.visibility = View.VISIBLE
        container.removeAllViews()
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { v.list(module.module, limit = 50) }
            }
            result.onSuccess { rows -> render(rows) }
                .onFailure { status.text = getString(R.string.load_failed, it.message ?: "") }
        }
    }

    private fun render(rows: JSONArray) {
        if (rows.length() == 0) {
            status.text = getString(R.string.no_records); return
        }
        status.visibility = View.GONE
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            val row = layoutInflater.inflate(R.layout.item_record, container, false)
            row.findViewById<TextView>(R.id.recTitle).text = titleOf(r)
            val sub = subtitleOf(r)
            row.findViewById<TextView>(R.id.recSubtitle).apply {
                text = sub
                visibility = if (sub.isEmpty()) View.GONE else View.VISIBLE
            }
            row.setOnClickListener {
                startActivity(
                    Intent(this, DetailActivity::class.java)
                        .putExtra("module", module.key)
                        .putExtra("id", r.optString("id"))
                )
            }
            container.addView(row)
        }
    }

    private fun titleOf(r: JSONObject): String {
        val t = module.titleFields.map { r.optString(it) }.filter { it.isNotBlank() }.joinToString(" ")
        return t.ifBlank { r.optString("id") }
    }

    private fun subtitleOf(r: JSONObject): String =
        module.subtitleFields.map { r.optString(it) }.filter { it.isNotBlank() }.joinToString("  ·  ")

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, getString(R.string.add)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> { finish(); return true }
            1 -> {
                startActivity(Intent(this, CreateActivity::class.java).putExtra("module", module.key))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
