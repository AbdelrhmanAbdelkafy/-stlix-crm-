package com.stlixvalley.crm.ui

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
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

/**
 * Generic create form. Reads the module's field metadata and shows an input for
 * each mandatory, editable, text-like field. Reference fields like the owner are
 * defaulted to the logged-in user; the server applies its own defaults for the rest.
 */
class CreateActivity : AppCompatActivity() {

    private val app get() = App.instance
    private val scope = MainScope()
    private lateinit var module: CrmModule
    private val inputs = LinkedHashMap<String, EditText>()
    private val users = ArrayList<Pair<String, String>>() // id -> display name
    private var ownerSpinner: Spinner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        module = Modules.byKey(intent.getStringExtra("module") ?: "contacts")
        setContentView(R.layout.activity_create)
        title = getString(R.string.add_to, module.title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val form = findViewById<LinearLayout>(R.id.form)
        val status = findViewById<TextView>(R.id.txtStatus)
        val save = findViewById<Button>(R.id.btnSave)
        val v = app.vtiger ?: run { finish(); return }

        save.isEnabled = false
        scope.launch {
            val loaded = withContext(Dispatchers.IO) {
                val fields = runCatching { editableFields(v.describe(module.module)) }.getOrDefault(emptyList())
                val us = runCatching {
                    parseUsers(v.query("SELECT id, first_name, last_name, user_name FROM Users;"))
                }.getOrDefault(emptyList())
                fields to us
            }
            val (fields, us) = loaded
            if (fields.isEmpty()) { status.text = getString(R.string.load_failed, ""); return@launch }
            status.visibility = View.GONE
            if (us.isNotEmpty()) {
                users.addAll(us)
                addOwnerSpinner(form, v.userId)
            }
            for (f in fields) {
                val row = layoutInflater.inflate(R.layout.item_input, form, false)
                row.findViewById<TextView>(R.id.inputLabel).text = f.label + if (f.mandatory) " *" else ""
                val edit = row.findViewById<EditText>(R.id.inputValue)
                edit.inputType = when (f.type) {
                    "email" -> InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    "phone" -> InputType.TYPE_CLASS_PHONE
                    "integer", "double", "currency" -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    else -> InputType.TYPE_CLASS_TEXT
                }
                inputs[f.name] = edit
                form.addView(row)
            }
            save.isEnabled = true
        }

        save.setOnClickListener {
            val element = JSONObject()
            for ((name, edit) in inputs) {
                val t = edit.text.toString().trim()
                if (t.isNotEmpty()) element.put(name, t)
            }
            // owner reference -> the chosen assignee, else the current user
            val owner = ownerSpinner?.let { users.getOrNull(it.selectedItemPosition)?.first } ?: v.userId
            owner?.let { element.put("assigned_user_id", it) }

            save.isEnabled = false
            status.visibility = View.VISIBLE
            status.text = getString(R.string.saving)
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    runCatching { v.create(module.module, element) }
                }
                res.onSuccess { finish() }
                    .onFailure { status.text = getString(R.string.save_failed, it.message ?: ""); save.isEnabled = true }
            }
        }
    }

    private fun parseUsers(arr: JSONArray): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        for (i in 0 until arr.length()) {
            val u = arr.getJSONObject(i)
            val name = listOf(u.optString("first_name"), u.optString("last_name"))
                .filter { it.isNotBlank() }.joinToString(" ")
                .ifBlank { u.optString("user_name") }
            val id = u.optString("id")
            if (id.isNotBlank()) out.add(id to name)
        }
        return out
    }

    /** Dropdown to assign the new record to any user; defaults to the current one. */
    private fun addOwnerSpinner(form: LinearLayout, currentUserId: String?) {
        val label = TextView(this).apply {
            text = getString(R.string.assign_to)
            setTextColor(0xFF0D2534.toInt())
            textSize = 13f
        }
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@CreateActivity, android.R.layout.simple_spinner_dropdown_item,
                users.map { it.second },
            )
        }
        users.indexOfFirst { it.first == currentUserId }.takeIf { it >= 0 }?.let { spinner.setSelection(it) }
        ownerSpinner = spinner
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT,
            )
            lp.bottomMargin = (12 * resources.displayMetrics.density).toInt()
            layoutParams = lp
            addView(label)
            addView(spinner)
        }
        form.addView(wrap, 0)
    }

    private data class Field(val name: String, val label: String, val type: String, val mandatory: Boolean)

    /** Editable text-like fields; mandatory first. Skips references/owner (auto-set). */
    private fun editableFields(describe: JSONObject): List<Field> {
        val out = ArrayList<Field>()
        val arr = describe.optJSONArray("fields") ?: return out
        val textTypes = setOf("string", "text", "email", "phone", "integer", "double", "currency", "url")
        for (i in 0 until arr.length()) {
            val f = arr.getJSONObject(i)
            if (!f.optBoolean("editable", true)) continue
            val type = f.optJSONObject("type")?.optString("name") ?: "string"
            if (f.optString("name") == "assigned_user_id") continue
            if (type !in textTypes) continue
            out.add(Field(f.optString("name"), f.optString("label", f.optString("name")), type, f.optBoolean("mandatory")))
        }
        return out.sortedByDescending { it.mandatory }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) { finish(); return true }
        return super.onOptionsItemSelected(item)
    }
}
