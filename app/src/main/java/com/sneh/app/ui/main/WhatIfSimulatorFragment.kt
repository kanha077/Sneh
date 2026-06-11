package com.sneh.app.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import com.sneh.app.core.CycleUtils

class WhatIfSimulatorFragment : Fragment() {

    private lateinit var presetSkipMeals: TextView
    private lateinit var presetSleep5h: TextView
    private lateinit var presetHighStress: TextView

    private lateinit var predictionCard: View
    private lateinit var txtPredictionTitle: TextView
    private lateinit var txtPredictionBody: TextView
    private lateinit var txtHormoneScore: TextView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var cycleLength = 28
    private var lastPeriodDate = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_what_if, container, false)

        presetSkipMeals = view.findViewById(R.id.presetSkipMeals)
        presetSleep5h = view.findViewById(R.id.presetSleep5h)
        presetHighStress = view.findViewById(R.id.presetHighStress)

        predictionCard = view.findViewById(R.id.predictionCard)
        txtPredictionTitle = view.findViewById(R.id.txtPredictionTitle)
        txtPredictionBody = view.findViewById(R.id.txtPredictionBody)
        txtHormoneScore = view.findViewById(R.id.txtHormoneScore)

        presetSkipMeals.setOnClickListener { selectPreset("skip_meals") }
        presetSleep5h.setOnClickListener { selectPreset("sleep_5h") }
        presetHighStress.setOnClickListener { selectPreset("high_stress") }

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
                }
            }
    }

    private fun selectPreset(scenario: String) {
        // Reset all presets background
        val pendingBg = R.drawable.bg_task_pending
        val textBrown = ContextCompat.getColor(requireContext(), R.color.calendarBrown)

        presetSkipMeals.setBackgroundResource(pendingBg)
        presetSkipMeals.setTextColor(textBrown)
        presetSleep5h.setBackgroundResource(pendingBg)
        presetSleep5h.setTextColor(textBrown)
        presetHighStress.setBackgroundResource(pendingBg)
        presetHighStress.setTextColor(textBrown)

        // Highlight selected
        val activePreset = when (scenario) {
            "skip_meals" -> presetSkipMeals
            "sleep_5h" -> presetSleep5h
            else -> presetHighStress
        }
        activePreset.setBackgroundResource(R.drawable.bg_task_done)
        activePreset.setTextColor(Color.WHITE)

        evaluateScenario(scenario)
    }

    private fun evaluateScenario(scenario: String) {
        val (_, phase) = if (lastPeriodDate.isNotEmpty()) {
            CycleUtils.getCycleDayAndPhase(lastPeriodDate, cycleLength)
        } else {
            1 to "Unknown"
        }

        var title = ""
        var body = ""
        var score = 0

        when (scenario) {
            "skip_meals" -> {
                title = "Scenario: Skipping Meals 🍽️"
                when {
                    phase.contains("Menstrual") -> {
                        body = "Your iron and sugar levels fall. Skipping meals causes severe drops in energy, making cramps and irritability worse. Support your body with nutrient-rich foods."
                        score = 60
                    }
                    phase.contains("Follicular") -> {
                        body = "Estrogen is rising, so energy is relatively resilient. However, skipping meals can lead to sudden blood sugar dips and lethargy. Eat small, regular snacks."
                        score = 75
                    }
                    phase.contains("Ovulation") -> {
                        body = "Can raise stress hormones like cortisol, potentially disrupting the delicate balance required for a healthy LH peak. Prioritize wholesome meals."
                        score = 70
                    }
                    else -> { // Luteal
                        body = "Progesterone dominant phase. Skipping meals will trigger intense mood swings, fatigue, and strong sugar cravings. Highly discouraged."
                        score = 55
                    }
                }
            }
            "sleep_5h" -> {
                title = "Scenario: Sleep 5 Hours 😴"
                when {
                    phase.contains("Menstrual") -> {
                        body = "Hormones are low. Depriving your body of sleep prevents muscle recovery, making physical cramps feel stronger and increasing morning fatigue."
                        score = 50
                    }
                    phase.contains("Follicular") -> {
                        body = "Your energy is generally high, so you can push through a short sleep night. Compensate with extra hydration and a 15-minute afternoon rest."
                        score = 70
                    }
                    phase.contains("Ovulation") -> {
                        body = "Limits peak energy reserves, reducing physical performance during a time when your high-intensity workouts are usually optimal."
                        score = 65
                    }
                    else -> { // Luteal
                        body = "Spikes stress hormones (cortisol) and intensifies PMS related anxiety, irritability, fluid retention, and poor sleep quality next night."
                        score = 45
                    }
                }
            }
            "high_stress" -> {
                title = "Scenario: High Stress ⚡"
                when {
                    phase.contains("Menstrual") -> {
                        body = "Elevated cortisol acts as a pain amplifier, potentially making uterine contractions and cramps feel twice as intense. Focus on deep breathing."
                        score = 40
                    }
                    phase.contains("Follicular") -> {
                        body = "Can suppress follicle-stimulating hormone (FSH) release, leading to a delayed ovulation. Stress management is critical this week."
                        score = 65
                    }
                    phase.contains("Ovulation") -> {
                        body = "Stress can suppress the luteinizing hormone (LH) peak, which may delay or prevent egg release. Practice yoga or take a warm bath."
                        score = 55
                    }
                    else -> { // Luteal
                        body = "Triggers rapid emotional drops, increases physical PMS symptoms like bloating and headaches, and results in poor emotional regulation."
                        score = 35
                    }
                }
            }
        }

        txtPredictionTitle.text = title
        txtPredictionBody.text = body
        txtHormoneScore.text = "Simulated Wellness Score: $score/100"
        predictionCard.visibility = View.VISIBLE
    }
}
