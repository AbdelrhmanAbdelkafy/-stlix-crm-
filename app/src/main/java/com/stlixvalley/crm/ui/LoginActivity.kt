package com.stlixvalley.crm.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.stlixvalley.crm.App
import com.stlixvalley.crm.R
import com.stlixvalley.crm.data.Vtiger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Vtiger login: URL + username + web-service Access Key (My Preferences page,
 * not the normal password). On success the Vtiger client is stored on App and
 * the home screen opens.
 */
class LoginActivity : AppCompatActivity() {

    private val app get() = App.instance
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // already logged in this process? go straight home
        if (app.vtiger?.session != null) {
            startActivity(Intent(this, HomeActivity::class.java)); finish(); return
        }
        setContentView(R.layout.activity_login)

        val url = findViewById<EditText>(R.id.inputUrl)
        val user = findViewById<EditText>(R.id.inputUser)
        val key = findViewById<EditText>(R.id.inputKey)
        val status = findViewById<TextView>(R.id.txtStatus)
        val btn = findViewById<Button>(R.id.btnLogin)

        url.setText(app.config.url)
        user.setText(app.config.username)
        key.setText(app.config.accessKey)

        btn.setOnClickListener {
            val u = url.text.toString().trim()
            val n = user.text.toString().trim()
            val k = key.text.toString().trim()
            if (u.isEmpty() || n.isEmpty() || k.isEmpty()) {
                status.text = getString(R.string.fill_all_fields); return@setOnClickListener
            }
            btn.isEnabled = false
            status.text = getString(R.string.logging_in)
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val v = Vtiger(u)
                        v.login(n, k)
                        v
                    }
                }
                result.onSuccess { v ->
                    app.vtiger = v
                    app.config.url = u; app.config.username = n; app.config.accessKey = k
                    startActivity(Intent(this@LoginActivity, HomeActivity::class.java))
                    finish()
                }.onFailure {
                    status.text = getString(R.string.login_failed, it.message ?: "")
                    btn.isEnabled = true
                }
            }
        }
    }
}
