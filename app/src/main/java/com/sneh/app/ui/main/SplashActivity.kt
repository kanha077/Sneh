package com.sneh.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.sneh.app.R
import com.sneh.app.auth.LoginActivity
import com.sneh.app.onboarding.OnboardingActivity
import com.sneh.app.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logo)

        // 🌸 animation
        logo.alpha = 0f
        logo.scaleX = 0.8f
        logo.scaleY = 0.8f

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(800)
            .start()

        Handler(Looper.getMainLooper()).postDelayed({

            val user = auth.currentUser

            val prefs = getSharedPreferences("app", MODE_PRIVATE)
            val isOnboarded = prefs.getBoolean("onboarded", false)

            when {
                user == null -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                }
                !isOnboarded -> {
                    startActivity(Intent(this, OnboardingActivity::class.java))
                }
                else -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }

            finish()

        }, 1200)
    }
}