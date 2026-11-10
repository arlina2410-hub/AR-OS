package com.aros.app

import android.app.Application
import com.aros.app.core.AppContainer

class ArOsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: ArOsApplication
            private set
    }
}
