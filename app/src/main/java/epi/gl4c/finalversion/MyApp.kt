package epi.gl4c.finalversion

import android.app.Application
import com.cloudinary.android.MediaManager
import java.util.HashMap

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Cloudinary
        val config = HashMap<String, String>()
        config["cloud_name"] = "dip6whhie"
        config["api_key"] = "742521121998967"
        config["api_secret"] = "yrKK6LSpsjNMz9_yBoDP5hwUT9E"
        MediaManager.init(this, config)
    }
}
