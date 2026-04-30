package com.sneh.app.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import com.sneh.app.onboarding.OnboardingActivity

class SignupActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val signupBtn = findViewById<Button>(R.id.signupBtn)
        val goLogin = findViewById<TextView>(R.id.goLogin)

        signupBtn.setOnClickListener {

            val emailText = email.text.toString().trim()
            val passText = password.text.toString().trim()

            if (emailText.isEmpty() || passText.length < 6) {
                Toast.makeText(this, "Invalid input", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signupBtn.isEnabled = false
            signupBtn.text = "Creating..."

            auth.createUserWithEmailAndPassword(emailText, passText)
                .addOnSuccessListener {

                    val userId = auth.currentUser?.uid ?: return@addOnSuccessListener

                    // 🔥 CREATE USER DOC
                    val data = hashMapOf(
                        "onboarding" to mapOf("completed" to false)
                    )

                    db.collection("users").document(userId).set(data)

                    // 👉 go onboarding
                    startActivity(Intent(this, OnboardingActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "Signup failed", Toast.LENGTH_LONG).show()
                    signupBtn.isEnabled = true
                    signupBtn.text = "Sign Up"
                }
        }

        goLogin.setOnClickListener { finish() }
    }
}