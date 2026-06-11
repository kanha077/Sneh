package com.sneh.app.ui.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import com.sneh.app.auth.LoginActivity
import com.sneh.app.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        Handler(Looper.getMainLooper()).postDelayed({
            checkUserState()
        }, 1500)
    }

    private fun checkUserState() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Not logged in -> Go to Login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // Logged in -> Check onboarding
            val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)
            val isLocalOnboarded = prefs.getBoolean("onboarded", false)

            if (isLocalOnboarded) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                // Check Firestore as fallback
                db.collection("users").document(currentUser.uid).get()
                    .addOnSuccessListener { doc ->
                        val onboarding = doc.get("onboarding") as? Map<*, *>
                        val isFirestoreOnboarded = onboarding?.get("completed") as? Boolean ?: false

                        if (isFirestoreOnboarded) {
                            prefs.edit().putBoolean("onboarded", true).apply()
                            startActivity(Intent(this, MainActivity::class.java))
                        } else {
                            startActivity(Intent(this, OnboardingActivity::class.java))
                        }
                        finish()
                    }
                    .addOnFailureListener {
                        // Network error or doc doesn't exist -> Go to Onboarding
                        startActivity(Intent(this, OnboardingActivity::class.java))
                        finish()
                    }
            }
        }
    }
}
