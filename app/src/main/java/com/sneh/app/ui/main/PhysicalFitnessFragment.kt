package com.sneh.app.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import com.sneh.app.core.CycleUtils
import java.text.SimpleDateFormat
import java.util.*

class PhysicalFitnessFragment : Fragment() {

    private lateinit var txtFitnessPhase: TextView
    private lateinit var txtFitnessDescription: TextView
    private lateinit var txtWorkoutHistory: TextView

    private lateinit var txtWorkout1Name: TextView
    private lateinit var txtWorkout1Details: TextView
    private lateinit var txtWorkout2Name: TextView
    private lateinit var txtWorkout2Details: TextView
    private lateinit var txtWorkout3Name: TextView
    private lateinit var txtWorkout3Details: TextView

    private lateinit var btnLogWorkout1: TextView
    private lateinit var btnLogWorkout2: TextView
    private lateinit var btnLogWorkout3: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var cycleLength = 28
    private var lastPeriodDate = ""
    private val loggedWorkouts = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_physical_fitness, container, false)

        txtFitnessPhase = view.findViewById(R.id.txtFitnessPhase)
        txtFitnessDescription = view.findViewById(R.id.txtFitnessDescription)
        txtWorkoutHistory = view.findViewById(R.id.txtWorkoutHistory)

        txtWorkout1Name = view.findViewById(R.id.txtWorkout1Name)
        txtWorkout1Details = view.findViewById(R.id.txtWorkout1Details)
        txtWorkout2Name = view.findViewById(R.id.txtWorkout2Name)
        txtWorkout2Details = view.findViewById(R.id.txtWorkout2Details)
        txtWorkout3Name = view.findViewById(R.id.txtWorkout3Name)
        txtWorkout3Details = view.findViewById(R.id.txtWorkout3Details)

        btnLogWorkout1 = view.findViewById(R.id.btnLogWorkout1)
        btnLogWorkout2 = view.findViewById(R.id.btnLogWorkout2)
        btnLogWorkout3 = view.findViewById(R.id.btnLogWorkout3)

        btnLogWorkout1.setOnClickListener { logWorkout(txtWorkout1Name.text.toString()) }
        btnLogWorkout2.setOnClickListener { logWorkout(txtWorkout2Name.text.toString()) }
        btnLogWorkout3.setOnClickListener { logWorkout(txtWorkout3Name.text.toString()) }

        fetchUserData()

        return view
    }

    private fun fetchUserData() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded) {
                    val cycle = doc.get("cycle") as? Map<*, *>
                    cycleLength = (cycle?.get("cycleLength") as? Long)?.toInt() ?: 28
                    lastPeriodDate = cycle?.get("lastPeriodDate") as? String ?: ""

                    updateWorkoutsRecommendation()
                    fetchTodayWorkouts()
                }
            }
    }

    private fun updateWorkoutsRecommendation() {
        if (lastPeriodDate.isEmpty()) return

        val (_, phase) = CycleUtils.getCycleDayAndPhase(lastPeriodDate, cycleLength)
        txtFitnessPhase.text = "Fitness: $phase"

        when {
            phase.contains("Menstrual") -> {
                txtFitnessDescription.text = "Focus on easing cramps and gentle energy flow. Avoid high intensity activities."
                txtWorkout1Name.text = "🧘‍♀️ Gentle Yoga Nidra"
                txtWorkout1Details.text = "15 mins • Eases menstrual cramps"
                txtWorkout2Name.text = "🚶‍♀️ Steady Pace Walk"
                txtWorkout2Details.text = "30 mins • Restorative cardio"
                txtWorkout3Name.text = "🤸‍♀️ Slow Pelvic Stretches"
                txtWorkout3Details.text = "10 mins • Releases lower back tension"
            }
            phase.contains("Follicular") -> {
                txtFitnessDescription.text = "Estrogen is rising. Energy is building. Great time for resistance & light cardio."
                txtWorkout1Name.text = "🩰 Pilates Core Fusion"
                txtWorkout1Details.text = "20 mins • Tones muscles & improves posture"
                txtWorkout2Name.text = "🏃‍♀️ Light Interval Jog"
                txtWorkout2Details.text = "25 mins • Boosts endurance & mood"
                txtWorkout3Name.text = "💪 Bodyweight Circuit"
                txtWorkout3Details.text = "15 mins • Squats, lunges, planks"
            }
            phase.contains("Ovulation") -> {
                txtFitnessDescription.text = "Your energy peaks! Perfect time for high-intensity cardio, power and strength workouts."
                txtWorkout1Name.text = "🔥 HIIT Sweat Session"
                txtWorkout1Details.text = "20 mins • High intensity calorie burner"
                txtWorkout2Name.text = "🏋️‍♀️ Strength Lift Workout"
                txtWorkout2Details.text = "30 mins • High load muscle training"
                txtWorkout3Name.text = "🚴‍♀️ Power Cycling Sprint"
                txtWorkout3Details.text = "20 mins • Builds stamina & power"
            }
            else -> { // Luteal
                txtFitnessDescription.text = "Energy begins to wind down. Focus on Pilates, strength endurance, and restorative yoga."
                txtWorkout1Name.text = "🧘‍♀️ Slow Flow Vinyasa"
                txtWorkout1Details.text = "25 mins • Calms mind & body"
                txtWorkout2Name.text = "🤸‍♀️ Mat Pilates Legs"
                txtWorkout2Details.text = "20 mins • Lower body stability"
                txtWorkout3Name.text = "🚶‍♀️ Brisk Evening Walk"
                txtWorkout3Details.text = "30 mins • Relieves stress & relaxes"
            }
        }
    }

    private fun logWorkout(workoutName: String) {
        val currentUser = auth.currentUser ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        loggedWorkouts.add(workoutName)

        val updates = mapOf(
            "workouts" to loggedWorkouts,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(currentUser.uid)
            .collection("workoutLogs").document(todayKey).set(updates)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Workout logged successfully!", Toast.LENGTH_SHORT).show()
                    updateHistoryText()
                }
            }
    }

    private fun fetchTodayWorkouts() {
        val currentUser = auth.currentUser ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("users").document(currentUser.uid)
            .collection("workoutLogs").document(todayKey).get()
            .addOnSuccessListener { doc ->
                if (isAdded && doc.exists()) {
                    val list = doc.get("workouts") as? List<*>
                    loggedWorkouts.clear()
                    list?.filterIsInstance<String>()?.let {
                        loggedWorkouts.addAll(it)
                    }
                    updateHistoryText()
                }
            }
    }

    private fun updateHistoryText() {
        if (loggedWorkouts.isEmpty()) {
            txtWorkoutHistory.text = "No workouts logged today yet."
        } else {
            val sb = StringBuilder()
            loggedWorkouts.forEachIndexed { i, name ->
                sb.append("${i + 1}. $name (Completed)\n")
            }
            txtWorkoutHistory.text = sb.toString().trim()
        }
    }
}
