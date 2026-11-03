package shifa.uz

import android.app.Application
import shifa.uz.data.analytics.setUpAnalytics
import shifa.uz.data.storage.initSecureSettings

class ShifaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Before anything reads the token (MainActivity used to do this).
        initSecureSettings(this)
        setUpAnalytics(this)
    }
}
