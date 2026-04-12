package com.kheprix

import android.app.Application
import com.kheprix.api.SessionManager

class KheprixApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
