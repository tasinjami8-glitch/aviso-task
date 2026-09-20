package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class AvisoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initFirebase()
    }

    private fun initFirebase() {
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey("AIzaSyBoAyv1vCXyWKCqaBPLR_KWPacnpppVCNM")
                    .setApplicationId("1:705109167205:android:c26593fb10cd19729e8a8c")
                    .setProjectId("tasin-tournament-hub-1d925")
                    .setStorageBucket("tasin-tournament-hub-1d925.firebasestorage.app")
                    .setGcmSenderId("705109167205")
                    .build()

                FirebaseApp.initializeApp(this, options)
                Log.d("FirebaseSetup", "Firebase initialized successfully with project tasin-tournament-hub-1d925")
            }
        } catch (e: Exception) {
            Log.e("FirebaseSetup", "Failed to initialize Firebase: ${e.message}", e)
        }
    }
}
