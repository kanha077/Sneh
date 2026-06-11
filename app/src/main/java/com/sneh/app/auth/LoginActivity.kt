package com.sneh.app.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import com.sneh.app.onboarding.OnboardingActivity
import com.sneh.app.ui.main.MainActivity

class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val emailEdit = findViewById<EditText>(R.id.email)
        val passwordEdit = findViewById<EditText>(R.id.password)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val googleBtn = findViewById<Button>(R.id.googleBtn)
        val goSignup = findViewById<TextView>(R.id.goSignup)
        val forgotPassword = findViewById<TextView>(R.id.forgotPassword)

        loginBtn.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    checkOnboardingAndRedirect()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        googleBtn.setOnClickListener {
            // Simulated Google Sign-in for demo
            Toast.makeText(this, "Google Sign-In Clicked! (Demo Mode)", Toast.LENGTH_SHORT).show()
        }

        goSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        forgotPassword.setOnClickListener {
            val email = emailEdit.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email in the email field first", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error sending reset email: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun checkOnboardingAndRedirect() {
        val user = auth.currentUser ?: return
        val prefs = getSharedPreferences("app", Context.MODE_PRIVATE)

        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { doc ->
                val onboarding = doc.get("onboarding") as? Map<*, *>
                val isCompleted = onboarding?.get("completed") as? Boolean ?: false

                if (isCompleted) {
                    prefs.edit().putBoolean("onboarded", true).apply()
                    startActivity(Intent(this, MainActivity::class.java))
                } else {
                    startActivity(Intent(this, OnboardingActivity::class.java))
                }
                finish()
            }
            .addOnFailureListener {
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
            }
    }
}
