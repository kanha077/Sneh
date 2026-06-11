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

        val nameEdit = findViewById<EditText>(R.id.name)
        val emailEdit = findViewById<EditText>(R.id.email)
        val passwordEdit = findViewById<EditText>(R.id.password)
        val confirmPasswordEdit = findViewById<EditText>(R.id.confirmPassword)
        val signupBtn = findViewById<Button>(R.id.signupBtn)
        val goLogin = findViewById<TextView>(R.id.goLogin)

        signupBtn.setOnClickListener {
            val name = nameEdit.text.toString().trim()
            val email = emailEdit.text.toString().trim()
            val password = passwordEdit.text.toString().trim()
            val confirmPassword = confirmPasswordEdit.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val user = result.user
                    if (user != null) {
                        // Initialize user profile in Firestore
                        val data = hashMapOf(
                            "profile" to mapOf(
                                "name" to name,
                                "email" to email,
                                "age" to 0,
                                "activityLevel" to ""
                            ),
                            "cycle" to mapOf(
                                "cycleLength" to 28,
                                "periodLength" to 5,
                                "lastPeriodDate" to "",
                                "regularity" to ""
                            ),
                            "onboarding" to mapOf("completed" to false)
                        )

                        db.collection("users").document(user.uid).set(data)
                            .addOnSuccessListener {
                                startActivity(Intent(this, OnboardingActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                // Continue even if DB profile init fails temporarily
                                startActivity(Intent(this, OnboardingActivity::class.java))
                                finish()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        goLogin.setOnClickListener {
            finish()
        }
    }
}
