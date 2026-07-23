package com.stlixvalley.crm

import android.app.Application
import com.stlixvalley.crm.data.Config
import com.stlixvalley.crm.data.Vtiger

class App : Application() {

    lateinit var config: Config
        private set

    /** The active Vtiger client for the current session (null until logged in). */
    var vtiger: Vtiger? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        config = Config(this)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
