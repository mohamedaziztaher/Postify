package epi.gl4c.finalversion

import android.app.Application
import com.cloudinary.android.MediaManager

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val cloudinaryConfig = mapOf(
            "cloud_name" to "dip6whhie",
            "api_key" to "742521121998967",
            "api_secret" to "yrKK6LSpsjNMz9_yBoDP5hwUT9E"
        )
        MediaManager.init(this, cloudinaryConfig)
    }
}
