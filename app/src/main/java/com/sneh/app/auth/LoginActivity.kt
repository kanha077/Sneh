package com.sneh.app.auth

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

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val goSignup = findViewById<TextView>(R.id.goSignup)

        loginBtn.setOnClickListener {

            val emailText = email.text.toString().trim()
            val passText = password.text.toString().trim()

            if (emailText.isEmpty() || passText.length < 6) {
                Toast.makeText(this, "Enter valid credentials", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginBtn.isEnabled = false
            loginBtn.text = "Logging in..."

            // 🔥 FIREBASE LOGIN
            auth.signInWithEmailAndPassword(emailText, passText)
                .addOnSuccessListener {

                    val userId = auth.currentUser?.uid ?: return@addOnSuccessListener

                    // 🔍 CHECK ONBOARDING STATUS
                    db.collection("users")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { doc ->

                            val onboarding = doc.get("onboarding") as? Map<*, *>
                            val isCompleted = onboarding?.get("completed") as? Boolean ?: false

                            if (isCompleted) {
                                startActivity(Intent(this, MainActivity::class.java))
                            } else {
                                startActivity(Intent(this, OnboardingActivity::class.java))
                            }

                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error loading profile", Toast.LENGTH_LONG).show()
                            loginBtn.isEnabled = true
                            loginBtn.text = "Login"
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "Login failed", Toast.LENGTH_LONG).show()
                    loginBtn.isEnabled = true
                    loginBtn.text = "Login"
                }
        }

        goSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}