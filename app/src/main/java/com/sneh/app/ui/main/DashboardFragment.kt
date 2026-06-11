package com.sneh.app.ui.main

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import com.sneh.app.auth.LoginActivity
import com.sneh.app.core.CycleUtils
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var menuBtn: ImageView
    private lateinit var greetingText: TextView
    private lateinit var txtCycleDay: TextView
    private lateinit var txtCyclePhase: TextView
    private lateinit var txtPhaseRecommendation: TextView

    // Correction card views
    private lateinit var periodCorrectionCard: View
    private lateinit var correctionLogBtn: Button
    private lateinit var correctionDatePickerBtn: Button

    // Tasks checkmarks
    private lateinit var imgTaskWaterCheck: ImageView
    private lateinit var imgTaskMedCheck: ImageView
    private lateinit var imgTaskSymCheck: ImageView

    private var waterChecked = false
    private var medChecked = false
    private var symChecked = false

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Loaded cycle details
    private var cycleLength = 28
    private var lastPeriodDate = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Bind Views
        drawerLayout = view.findViewById(R.id.drawerLayout)
        menuBtn = view.findViewById(R.id.menuBtn)
        greetingText = view.findViewById(R.id.greetingText)
        txtCycleDay = view.findViewById(R.id.txtCycleDay)
        txtCyclePhase = view.findViewById(R.id.txtCyclePhase)
        txtPhaseRecommendation = view.findViewById(R.id.txtPhaseRecommendation)

        periodCorrectionCard = view.findViewById(R.id.periodCorrectionCard)
        correctionLogBtn = view.findViewById(R.id.correctionLogBtn)
        correctionDatePickerBtn = view.findViewById(R.id.correctionDatePickerBtn)

        imgTaskWaterCheck = view.findViewById(R.id.imgTaskWaterCheck)
        imgTaskMedCheck = view.findViewById(R.id.imgTaskMedCheck)
        imgTaskSymCheck = view.findViewById(R.id.imgTaskSymCheck)

        val quickLogBtn = view.findViewById<Button>(R.id.quickLogBtn)

        // Menu buttons
        menuBtn.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        setupDrawerNavigation(view)

        // Today Tasks click triggers to respective tabs
        view.findViewById<View>(R.id.taskWater).setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.tabNutrition)
        }
        view.findViewById<View>(R.id.taskMeditation).setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.tabMind)
        }
        view.findViewById<View>(R.id.taskSymptoms).setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.tabCycle)
        }

        // Quick Log navigates to Calendar
        quickLogBtn.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.tabCycle)
        }

        // Period correction actions
        correctionLogBtn.setOnClickListener {
            updatePeriodDateToday()
        }

        correctionDatePickerBtn.setOnClickListener {
            showDatePicker()
        }

        // Fetch Data from Firestore
        fetchUserData()

        return view
    }

    private fun fetchUserData() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded && context != null) {
                    val profile = doc.get("profile") as? Map<*, *>
                    val userName = profile?.get("name") as? String ?: "Priya"
                    greetingText.text = "Hello, $userName 🌸"

                    val cycle = doc.get("cycle") as? Map<*, *>
                    cycleLength = (cycle?.get("cycleLength") as? Long)?.toInt() ?: 28
                    lastPeriodDate = cycle?.get("lastPeriodDate") as? String ?: ""

                    updateCycleUI()

                    // Check today's symptoms from user doc symptomLogs map
                    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val symptomLogs = doc.get("symptomLogs") as? Map<*, *>
                    val todaySymptoms = symptomLogs?.get(todayKey) as? List<*>
                    symChecked = todaySymptoms != null && todaySymptoms.isNotEmpty()
                    imgTaskSymCheck.setImageResource(if (symChecked) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)

                    fetchTodayTasks(currentUser.uid)
                }
            }
    }

    private fun fetchTodayTasks(userId: String) {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 1. Water Glasses Log
        db.collection("users").document(userId)
            .collection("dailyWater").document(todayKey).get()
            .addOnSuccessListener { doc ->
                if (isAdded) {
                    val glasses = doc.getLong("waterGlasses") ?: 0L
                    waterChecked = glasses >= 8
                    imgTaskWaterCheck.setImageResource(if (waterChecked) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)
                }
            }

        // 2. Meditation Session Log
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis

        db.collection("users").document(userId)
            .collection("wellnessLogs")
            .whereGreaterThanOrEqualTo("timestamp", startOfDay)
            .get()
            .addOnSuccessListener { docs ->
                if (isAdded) {
                    var foundMed = false
                    for (doc in docs) {
                        if (doc.getString("activity") == "Meditation") {
                            foundMed = true
                            break
                        }
                    }
                    medChecked = foundMed
                    imgTaskMedCheck.setImageResource(if (medChecked) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)
                }
            }
    }

    private fun updateCycleUI() {
        if (lastPeriodDate.isEmpty()) {
            txtCycleDay.text = "No Data"
            txtCyclePhase.text = "Set Last Period Date"
            txtPhaseRecommendation.text = "Please log your last period start date in onboarding or calendar."
            periodCorrectionCard.visibility = View.GONE
            return
        }

        val (day, phase) = CycleUtils.getCycleDayAndPhase(lastPeriodDate, cycleLength)

        txtCycleDay.text = "Day $day"
        txtCyclePhase.text = phase

        // Update recommendation based on phase name
        when {
            phase.contains("Menstrual") -> {
                txtPhaseRecommendation.text = "Estrogen and progesterone are low. Rest, gentle stretching or walking, and warm tea are highly recommended today."
                periodCorrectionCard.visibility = View.GONE
            }
            phase.contains("Follicular") -> {
                txtPhaseRecommendation.text = "Estrogen is rising! Your energy is building up. Perfect time for strength workouts or planning creative projects."
                periodCorrectionCard.visibility = View.GONE
            }
            phase.contains("Ovulation") -> {
                txtPhaseRecommendation.text = "Luteinizing hormone peaks. You are in your fertile window. Take advantage of high energy levels for high-intensity exercise."
                periodCorrectionCard.visibility = View.GONE
            }
            phase.contains("Luteal") -> {
                txtPhaseRecommendation.text = "Progesterone dominates. You may feel a wind-down. Focus on calming exercises, yoga, and magnesium-rich food."
                periodCorrectionCard.visibility = View.GONE
            }
            phase.contains("Overdue") -> {
                txtCycleDay.text = "Day $day"
                txtCyclePhase.text = "Late Period ⚠️"
                txtPhaseRecommendation.text = "You are past your predicted cycle length. If you started your period, please update it."
                // Show correction prompt card
                periodCorrectionCard.visibility = View.VISIBLE
            }
            else -> {
                txtPhaseRecommendation.text = "Focus on healthy meals, hydration, and regular sleep to support your body's natural flow."
                periodCorrectionCard.visibility = View.GONE
            }
        }
    }

    private fun updatePeriodDateToday() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        saveNewPeriodDate(todayStr)
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonth)
                set(Calendar.DAY_OF_MONTH, selectedDay)
            }
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = sdf.format(selCal.time)
            saveNewPeriodDate(dateStr)
        }, year, month, day)

        dpd.datePicker.maxDate = System.currentTimeMillis()
        dpd.show()
    }

    private fun saveNewPeriodDate(dateStr: String) {
        val currentUser = auth.currentUser ?: return
        val updates = mapOf(
            "cycle.lastPeriodDate" to dateStr
        )
        db.collection("users").document(currentUser.uid).update(updates)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Period start date updated!", Toast.LENGTH_SHORT).show()
                    lastPeriodDate = dateStr
                    updateCycleUI()
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(context, "Failed to update period date", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setupDrawerNavigation(view: View) {
        val menuAiChat = view.findViewById<TextView>(R.id.menuAiChat)
        val menuWhatIf = view.findViewById<TextView>(R.id.menuWhatIf)
        val menuFitness = view.findViewById<TextView>(R.id.menuFitness)
        val menuFertility = view.findViewById<TextView>(R.id.menuFertility)
        val menuCommunity = view.findViewById<TextView>(R.id.menuCommunity)
        val menuReport = view.findViewById<TextView>(R.id.menuReport)
        val menuLogout = view.findViewById<TextView>(R.id.menuLogout)

        menuAiChat.setOnClickListener {
            closeDrawerAndSwitch(AiChatFragment())
        }
        menuWhatIf.setOnClickListener {
            closeDrawerAndSwitch(WhatIfSimulatorFragment())
        }
        menuFitness.setOnClickListener {
            closeDrawerAndSwitch(PhysicalFitnessFragment())
        }
        menuFertility.setOnClickListener {
            closeDrawerAndSwitch(FertilityPlanFragment())
        }
        menuCommunity.setOnClickListener {
            closeDrawerAndSwitch(ExpertCommunityFragment())
        }
        menuReport.setOnClickListener {
            (activity as? MainActivity)?.navigateToTab(R.id.tabMe)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
        menuLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val prefs = requireContext().getSharedPreferences("app", Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
            
            val intent = Intent(requireActivity(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun closeDrawerAndSwitch(fragment: Fragment) {
        drawerLayout.closeDrawer(GravityCompat.START)
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
