package com.sneh.app.auth

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import com.sneh.app.onboarding.OnboardingActivity

class SignupActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val signupBtn = findViewById<Button>(R.id.signupBtn)
        val googleBtn = findViewById<Button>(R.id.googleBtn)
        val goLogin = findViewById<TextView>(R.id.goLogin)

        // 🌈 Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
                        "onboarding" to mapOf("completed" to false),
                        "email" to emailText,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(userId).set(data)
                        .addOnSuccessListener {
                            // 👉 Successfully created profile, go to onboarding
                            startActivity(Intent(this, OnboardingActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Profile creation failed: ${e.message}", Toast.LENGTH_LONG).show()
                            signupBtn.isEnabled = true
                            signupBtn.text = "Sign Up"
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, e.message ?: "Signup failed", Toast.LENGTH_LONG).show()
                    signupBtn.isEnabled = true
                    signupBtn.text = "Sign Up"
                }
        }

        googleBtn.setOnClickListener {
            signInWithGoogle()
        }

        goLogin.setOnClickListener { finish() }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleLauncher.launch(signInIntent)
    }

    private val googleLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                checkOnboardingStatus()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkOnboardingStatus() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val onboarding = doc.get("onboarding") as? Map<*, *>
                    val isCompleted = onboarding?.get("completed") as? Boolean ?: false
                    if (isCompleted) {
                        // This shouldn't happen on signup but added for safety
                        val intent = Intent(this, com.sneh.app.ui.main.MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        startActivity(Intent(this, OnboardingActivity::class.java))
                    }
                } else {
                    // Create basic profile for Google user
                    val data = hashMapOf(
                        "onboarding" to mapOf("completed" to false),
                        "email" to auth.currentUser?.email,
                        "createdAt" to System.currentTimeMillis()
                    )
                    db.collection("users").document(userId).set(data)
                    startActivity(Intent(this, OnboardingActivity::class.java))
                }
                finish()
            }
    }
}