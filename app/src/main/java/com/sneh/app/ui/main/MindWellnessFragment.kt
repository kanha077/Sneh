package com.sneh.app.ui.main

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import java.text.SimpleDateFormat
import java.util.*

class MindWellnessFragment : Fragment() {

    private lateinit var txtLoggedMood: TextView
    private lateinit var txtTimer: TextView
    private lateinit var btnPlayMeditation: Button

    private lateinit var moodHappy: TextView
    private lateinit var moodCalm: TextView
    private lateinit var moodSad: TextView
    private lateinit var moodStressed: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var countdownTimer: CountDownTimer? = null
    private var timerRunning = false
    private var timeLeftMs: Long = 480000 // 8 minutes

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mind_wellness, container, false)

        txtLoggedMood = view.findViewById(R.id.txtLoggedMood)
        txtTimer = view.findViewById(R.id.txtTimer)
        btnPlayMeditation = view.findViewById(R.id.btnPlayMeditation)

        moodHappy = view.findViewById(R.id.moodHappy)
        moodCalm = view.findViewById(R.id.moodCalm)
        moodSad = view.findViewById(R.id.moodSad)
        moodStressed = view.findViewById(R.id.moodStressed)

        moodHappy.setOnClickListener { logMood("Happy 😊") }
        moodCalm.setOnClickListener { logMood("Calm 😌") }
        moodSad.setOnClickListener { logMood("Sad 😔") }
        moodStressed.setOnClickListener { logMood("Stressed 😠") }

        btnPlayMeditation.setOnClickListener {
            if (timerRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        fetchTodayMood()

        return view
    }

    private fun logMood(mood: String) {
        val currentUser = auth.currentUser ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val updates = mapOf(
            "mood" to mood,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(currentUser.uid)
            .collection("moodLogs").document(todayKey).set(updates)
            .addOnSuccessListener {
                if (isAdded) {
                    txtLoggedMood.text = "Today's mood: $mood"
                    Toast.makeText(context, "Mood logged successfully!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchTodayMood() {
        val currentUser = auth.currentUser ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("users").document(currentUser.uid)
            .collection("moodLogs").document(todayKey).get()
            .addOnSuccessListener { doc ->
                if (isAdded && doc.exists()) {
                    val mood = doc.getString("mood") ?: ""
                    if (mood.isNotEmpty()) {
                        txtLoggedMood.text = "Today's mood: $mood"
                    }
                }
            }
    }

    private fun startTimer() {
        countdownTimer = object : CountDownTimer(timeLeftMs, 1000) {
            override fun onTick(msUntilFinished: Long) {
                timeLeftMs = msUntilFinished
                updateTimerText()
            }

            override fun onFinish() {
                timerRunning = false
                btnPlayMeditation.text = "Start Session"
                txtTimer.text = "Finished! 🌸"
                timeLeftMs = 480000
                logMeditationCompletion()
            }
        }.start()

        timerRunning = true
        btnPlayMeditation.text = "Pause"
    }

    private fun pauseTimer() {
        countdownTimer?.cancel()
        timerRunning = false
        btnPlayMeditation.text = "Resume"
    }

    private fun updateTimerText() {
        val minutes = (timeLeftMs / 1000) / 60
        val seconds = (timeLeftMs / 1000) % 60
        txtTimer.text = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun logMeditationCompletion() {
        val currentUser = auth.currentUser ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val updates = mapOf(
            "activity" to "Meditation",
            "durationMinutes" to 8,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(currentUser.uid)
            .collection("wellnessLogs").document().set(updates)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Meditation session logged in wellness history!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countdownTimer?.cancel()
    }
}
