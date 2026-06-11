package com.sneh.app.ui.main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import com.sneh.app.core.CycleUtils
import java.text.SimpleDateFormat
import java.util.*

class NutritionFragment : Fragment() {

    private lateinit var txtWaterProgress: TextView
    private lateinit var waterProgressBar: ProgressBar
    private lateinit var txtDietPhase: TextView
    private lateinit var txtDietAdvice: TextView

    private lateinit var foodChip1: TextView
    private lateinit var foodChip2: TextView
    private lateinit var foodChip3: TextView

    private lateinit var droplets: List<ImageView>

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var cycleLength = 28
    private var lastPeriodDate = ""
    private var waterLogged = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_nutrition, container, false)

        txtWaterProgress = view.findViewById(R.id.txtWaterProgress)
        waterProgressBar = view.findViewById(R.id.waterProgressBar)
        txtDietPhase = view.findViewById(R.id.txtDietPhase)
        txtDietAdvice = view.findViewById(R.id.txtDietAdvice)

        foodChip1 = view.findViewById(R.id.foodChip1)
        foodChip2 = view.findViewById(R.id.foodChip2)
        foodChip3 = view.findViewById(R.id.foodChip3)

        // Bind droplets
        droplets = listOf(
            view.findViewById(R.id.drop1),
            view.findViewById(R.id.drop2),
            view.findViewById(R.id.drop3),
            view.findViewById(R.id.drop4),
            view.findViewById(R.id.drop5),
            view.findViewById(R.id.drop6),
            view.findViewById(R.id.drop7),
            view.findViewById(R.id.drop8)
        )

        // Droplet clicks
        droplets.forEachIndexed { index, drop ->
            drop.setOnClickListener {
                setWaterIntake(index + 1)
            }
        }

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

                    updateDietAdvice()
                    fetchTodayWater()
                }
            }
    }

    private fun updateDietAdvice() {
        if (lastPeriodDate.isEmpty()) return

        val (_, phase) = CycleUtils.getCycleDayAndPhase(lastPeriodDate, cycleLength)
        txtDietPhase.text = "Diet Advice: $phase"

        when {
            phase.contains("Menstrual") -> {
                txtDietAdvice.text = "Your iron levels drop during menstruation. Focus on iron-rich foods, Vitamin C for absorption, and avoid caffeine."
                foodChip1.text = "Spinach 🥬"
                foodChip2.text = "Lentils 🍲"
                foodChip3.text = "Salmon 🐟"
            }
            phase.contains("Follicular") -> {
                txtDietAdvice.text = "Estrogen is rising. Support metabolic rates with fermented foods, dense protein, and plenty of cruciferous vegetables."
                foodChip1.text = "Broccoli 🥦"
                foodChip2.text = "Greek Yogurt 🍨"
                foodChip3.text = "Oats 🥣"
            }
            phase.contains("Ovulation") -> {
                txtDietAdvice.text = "Support egg health and estrogen metabolism. Focus on high fiber foods, berries, and omega-3 fatty acids."
                foodChip1.text = "Berries 🍓"
                foodChip2.text = "Quinoa 🌾"
                foodChip3.text = "Walnuts 🥜"
            }
            else -> { // Luteal
                txtDietAdvice.text = "Progesterone peaks. You may crave sweets. Eat slow-release carbs, magnesium-rich foods (banana, dark chocolate) to stabilize blood sugar."
                foodChip1.text = "Dark Chocolate 🍫"
                foodChip2.text = "Banana 🍌"
                foodChip3.text = "Brown Rice 🍚"
            }
        }
    }

    private fun setWaterIntake(glasses: Int) {
        val currentUser = auth.currentUser ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        waterLogged = glasses
        updateWaterUI()

        val updates = mapOf(
            "waterGlasses" to waterLogged,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(currentUser.uid)
            .collection("dailyWater").document(todayKey).set(updates)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Water intake logged!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun fetchTodayWater() {
        val currentUser = auth.currentUser ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        db.collection("users").document(currentUser.uid)
            .collection("dailyWater").document(todayKey).get()
            .addOnSuccessListener { doc ->
                if (isAdded && doc.exists()) {
                    waterLogged = (doc.getLong("waterGlasses") ?: 0).toInt()
                    updateWaterUI()
                }
            }
    }

    private fun updateWaterUI() {
        txtWaterProgress.text = "Water Log: $waterLogged / 8 glasses"
        waterProgressBar.progress = waterLogged

        droplets.forEachIndexed { index, drop ->
            if (index < waterLogged) {
                drop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.waterBlue))
            } else {
                drop.setColorFilter(ContextCompat.getColor(requireContext(), R.color.snehMuted))
            }
        }
    }
}
