package com.stlixvalley.crm.ui

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stlixvalley.crm.App
import com.stlixvalley.crm.R
import com.stlixvalley.crm.data.Modules

/** Home: the three CRM modules as large cards. */
class HomeActivity : AppCompatActivity() {

    private val app get() = App.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // not logged in (e.g. process restart) -> back to login
        if (app.vtiger?.session == null) {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }
        setContentView(R.layout.activity_home)
        title = getString(R.string.app_name)

        val container = findViewById<LinearLayout>(R.id.moduleList)
        for (m in Modules.all) {
            val card = layoutInflater.inflate(R.layout.item_module, container, false)
            card.findViewById<TextView>(R.id.moduleTitle).text = "${m.emoji}  ${m.title}"
            card.setOnClickListener {
                startActivity(
                    Intent(this, ListActivity::class.java).putExtra("module", m.key)
                )
            }
            container.addView(card)
        }
    }
}
