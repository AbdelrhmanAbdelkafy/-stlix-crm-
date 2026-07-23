package com.stlixvalley.crm.data

import android.content.Context

/** Stored CRM connection settings (URL + credentials). */
class Config(context: Context) {

    private val sp = context.getSharedPreferences("crm", Context.MODE_PRIVATE)

    var url: String
        get() = sp.getString("url", "https://crm.stlixvalley.com")!!
        set(v) = sp.edit().putString("url", v.trim().trimEnd('/')).apply()

    var username: String
        get() = sp.getString("username", "")!!
        set(v) = sp.edit().putString("username", v.trim()).apply()

    var accessKey: String
        get() = sp.getString("accessKey", "")!!
        set(v) = sp.edit().putString("accessKey", v.trim()).apply()

    val isConfigured: Boolean
        get() = username.isNotEmpty() && accessKey.isNotEmpty()

    fun clear() = sp.edit().clear().apply()
}
