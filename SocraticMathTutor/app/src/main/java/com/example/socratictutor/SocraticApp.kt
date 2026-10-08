package com.example.socratictutor

import android.app.Application

class SocraticApp : Application() {
    lateinit var settings: SettingsStore; private set
    lateinit var progress: ProgressStore; private set

    override fun onCreate() {
        super.onCreate()
        settings = SettingsStore(this)
        progress = ProgressStore(this)
    }
}
