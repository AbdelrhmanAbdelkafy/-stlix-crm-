package com.stlixvalley.crm.ui

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.stlixvalley.crm.App
import com.stlixvalley.crm.R
import com.stlixvalley.crm.data.Modules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Combined activities — Tasks (Calendar) + Events + Tickets (HelpDesk) in ONE
 * timeline, newest first, each tagged by type. This is the "combo" view that
 * Vtiger's own app splits across three screens.
 */
class ActivitiesActivity : AppCompatActivity() {

    private val app get() = App.instance
    private val scope = MainScope()
    private lateinit var container: LinearLayout
    private lateinit var status: TextView

    private data class Item(
        val type: String, val color: Int, val moduleKey: String,
        val id: String, val title: String, val date: String, val sub: String,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        title = getString(R.string.activities)
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
        status.text = getString(R.string.loading); status.visibility = View.VISIBLE
        container.removeAllViews()
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                val out = ArrayList<Item>()
                runCatching {
                    val tasks = v.query("SELECT subject, date_start, taskstatus FROM Calendar LIMIT 40;")
                    add(out, tasks, "مهمة", 0xFF2E7D9A.toInt(), "tasks", "subject", "date_start", "taskstatus")
                }
                runCatching {
                    val events = v.query("SELECT subject, date_start, eventstatus FROM Events LIMIT 40;")
                    add(out, events, "حدث", 0xFF6A4C93.toInt(), "events", "subject", "date_start", "eventstatus")
                }
                runCatching {
                    val tickets = v.query("SELECT ticket_title, createdtime, ticketstatus FROM HelpDesk LIMIT 40;")
                    add(out, tickets, "تيكت", 0xFFF05223.toInt(), "tickets", "ticket_title", "createdtime", "ticketstatus")
                }
                out.sortedByDescending { it.date }
            }
            render(items)
        }
    }

    private fun add(
        out: ArrayList<Item>, rows: JSONArray, type: String, color: Int, moduleKey: String,
        titleF: String, dateF: String, subF: String,
    ) {
        for (i in 0 until rows.length()) {
            val r = rows.getJSONObject(i)
            out.add(
                Item(
                    type, color, moduleKey, r.optString("id"),
                    r.optString(titleF).ifBlank { "—" },
                    r.optString(dateF), r.optString(subF),
                )
            )
        }
    }

    private fun render(items: List<Item>) {
        if (items.isEmpty()) { status.text = getString(R.string.no_records); return }
        status.visibility = View.GONE
        for (item in items) {
            val row = layoutInflater.inflate(R.layout.item_activity, container, false)
            row.findViewById<TextView>(R.id.actType).apply {
                text = item.type; setBackgroundColor(item.color)
            }
            row.findViewById<TextView>(R.id.actTitle).text = item.title
            row.findViewById<TextView>(R.id.actMeta).text =
                listOf(item.date, item.sub).filter { s -> s.isNotBlank() }.joinToString("  ·  ")
            row.setOnClickListener {
                startActivity(
                    Intent(this, DetailActivity::class.java)
                        .putExtra("module", item.moduleKey).putExtra("id", item.id)
                )
            }
            container.addView(row)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, getString(R.string.add)).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> { finish(); return true }
            1 -> { pickTypeAndAdd(); return true }
        }
        return super.onOptionsItemSelected(item)
    }

    /** "+" on the combo asks which kind of activity to create, then opens the form. */
    private fun pickTypeAndAdd() {
        val keys = listOf("tasks", "events", "tickets")
        val labels = keys.map { k -> Modules.byKey(k).let { "${it.emoji}  ${it.title}" } }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle(R.string.add)
            .setItems(labels) { _, which ->
                startActivity(Intent(this, CreateActivity::class.java).putExtra("module", keys[which]))
            }
            .show()
    }
}
