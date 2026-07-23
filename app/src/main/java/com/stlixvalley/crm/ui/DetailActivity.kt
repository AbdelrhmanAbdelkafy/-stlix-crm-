package com.stlixvalley.crm.ui

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
                addActions(container, rec)
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

    /** Free, phone-native actions at the top of every record: WhatsApp (if there's
     *  a phone), Email (if there's an address), Add-to-calendar (if there's a date). */
    private fun addActions(container: LinearLayout, rec: JSONObject) {
        val name = listOf("firstname", "potentialname", "subject", "ticket_title")
            .map { rec.optString(it) }.firstOrNull { it.isNotBlank() } ?: module.title
        val phone = Actions.firstPhone(rec)
        val email = Actions.firstEmail(rec)
        val date = rec.optString("date_start")

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.bottomMargin = dp(8)
            layoutParams = lp
        }
        if (phone != null) bar.addView(actionButton(getString(R.string.whatsapp), 0xFF25D366.toInt()) {
            openIntent(Actions.whatsApp(phone, getString(R.string.whatsapp_greeting, name).trim()), R.string.whatsapp_missing)
        })
        if (email != null) bar.addView(actionButton(getString(R.string.send_email), 0xFF1A73E8.toInt()) {
            openIntent(Actions.email(email), R.string.email_missing)
        })
        if (date.isNotBlank()) bar.addView(actionButton(getString(R.string.add_to_calendar), 0xFF6A4C93.toInt()) {
            openIntent(Actions.calendar(name, module.title, date), R.string.calendar_missing)
        })
        if (bar.childCount > 0) container.addView(bar, 0)
    }

    private fun actionButton(label: String, color: Int, onClick: () -> Unit) =
        com.google.android.material.button.MaterialButton(this).apply {
            text = label
            setBackgroundColor(color)
            setTextColor(0xFFFFFFFF.toInt())
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.bottomMargin = dp(8)
            layoutParams = lp
            setOnClickListener { onClick() }
        }

    private fun openIntent(intent: Intent, failMsg: Int) {
        runCatching { startActivity(intent) }
            .onFailure { Toast.makeText(this, failMsg, Toast.LENGTH_LONG).show() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

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
