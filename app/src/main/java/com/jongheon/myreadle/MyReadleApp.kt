package com.jongheon.myreadle

import android.app.Application
import com.jongheon.myreadle.core.ServiceLocator

class MyReadleApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
