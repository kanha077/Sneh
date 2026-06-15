package com.sneh.app.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sneh.app.R
import com.sneh.app.core.CycleUtils

class WhatIfSimulatorFragment : Fragment() {

    private lateinit var tvHeaderCycleDay: TextView
    private lateinit var tvHeaderCyclePhase: TextView
    private lateinit var btnBack: View
    private lateinit var etWhatIfInput: TextInputEditText
    private lateinit var btnSimulate: MaterialButton
    
    private lateinit var chipSuggestion1: Chip
    private lateinit var chipSuggestion2: Chip
    private lateinit var chipSuggestion3: Chip
    private lateinit var chipSuggestion4: Chip
    
    private lateinit var loadingCard: MaterialCardView
    private lateinit var resultCard: MaterialCardView
    private lateinit var impactBadgeCard: MaterialCardView
    private lateinit var tvImpactBadge: TextView
    private lateinit var tvUserQuestion: TextView
    private lateinit var tvAiResponse: TextView
    private lateinit var tvAction1: TextView
    private lateinit var tvAction2: TextView
    private lateinit var tvAction3: TextView
    
    private lateinit var btnSaveResult: MaterialButton
    private lateinit var btnAskAnother: MaterialButton
    
    private lateinit var historyCard: MaterialCardView
    private lateinit var llHistoryContainer: LinearLayout
    private lateinit var tvClearHistory: TextView
    private lateinit var scrollView: NestedScrollView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var cycleLength = 28
    private var lastPeriodDate = ""
    private var currentCycleDay = 1
    private var currentCyclePhase = "Unknown"

    private var currentSimulationQuestion = ""
    private var currentSimulationResponse = ""
    private var currentSimulationRisk = "Low Risk"
    private var currentSimulationActions = listOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_what_if, container, false)

        // Bind Views
        tvHeaderCycleDay = view.findViewById(R.id.tvHeaderCycleDay)
        tvHeaderCyclePhase = view.findViewById(R.id.tvHeaderCyclePhase)
        btnBack = view.findViewById(R.id.btnBack)
        etWhatIfInput = view.findViewById(R.id.etWhatIfInput)
        btnSimulate = view.findViewById(R.id.btnSimulate)
        
        chipSuggestion1 = view.findViewById(R.id.chipSuggestion1)
        chipSuggestion2 = view.findViewById(R.id.chipSuggestion2)
        chipSuggestion3 = view.findViewById(R.id.chipSuggestion3)
        chipSuggestion4 = view.findViewById(R.id.chipSuggestion4)
        
        loadingCard = view.findViewById(R.id.loadingCard)
        resultCard = view.findViewById(R.id.resultCard)
        impactBadgeCard = view.findViewById(R.id.impactBadgeCard)
        tvImpactBadge = view.findViewById(R.id.tvImpactBadge)
        tvUserQuestion = view.findViewById(R.id.tvUserQuestion)
        tvAiResponse = view.findViewById(R.id.tvAiResponse)
        tvAction1 = view.findViewById(R.id.tvAction1)
        tvAction2 = view.findViewById(R.id.tvAction2)
        tvAction3 = view.findViewById(R.id.tvAction3)
        
        btnSaveResult = view.findViewById(R.id.btnSaveResult)
        btnAskAnother = view.findViewById(R.id.btnAskAnother)
        
        historyCard = view.findViewById(R.id.historyCard)
        llHistoryContainer = view.findViewById(R.id.llHistoryContainer)
        tvClearHistory = view.findViewById(R.id.tvClearHistory)
        scrollView = view.findViewById(R.id.scrollView)

        // Setup Suggestion Chips
        chipSuggestion1.setOnClickListener { fillSuggestion(chipSuggestion1.text.toString()) }
        chipSuggestion2.setOnClickListener { fillSuggestion(chipSuggestion2.text.toString()) }
        chipSuggestion3.setOnClickListener { fillSuggestion(chipSuggestion3.text.toString()) }
        chipSuggestion4.setOnClickListener { fillSuggestion(chipSuggestion4.text.toString()) }

        // Setup Listeners
        btnSimulate.setOnClickListener {
            val query = etWhatIfInput.text.toString().trim()
            simulatePrediction(query)
        }

        btnSaveResult.setOnClickListener {
            saveSimulation()
        }

        btnAskAnother.setOnClickListener {
            etWhatIfInput.text?.clear()
            resultCard.visibility = View.GONE
            etWhatIfInput.requestFocus()
        }

        tvClearHistory.setOnClickListener {
            clearHistory()
        }

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Fetch User Cycle Data and History
        fetchUserData()

        return view
    }

    private fun fillSuggestion(text: String) {
        etWhatIfInput.setText(text)
        etWhatIfInput.setSelection(text.length)
    }

    private fun fetchUserData() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded) {
                    val cycle = doc.get("cycle") as? Map<*, *>
                    cycleLength = (cycle?.get("cycleLength") as? Long)?.toInt() ?: 28
                    lastPeriodDate = cycle?.get("lastPeriodDate") as? String ?: ""

                    // Compute cycle day and phase
                    val (day, phase) = if (lastPeriodDate.isNotEmpty()) {
                        CycleUtils.getCycleDayAndPhase(lastPeriodDate, cycleLength)
                    } else {
                        1 to "Unknown"
                    }
                    currentCycleDay = day
                    currentCyclePhase = phase

                    tvHeaderCycleDay.text = "📅 Day $currentCycleDay"
                    tvHeaderCyclePhase.text = "🌸 $currentCyclePhase"

                    fetchPastSimulations(currentUser.uid)
                }
            }
    }

    private fun simulatePrediction(query: String) {
        val userQuestion = query.trim()
        if (userQuestion.isEmpty()) {
            Toast.makeText(context, "Please enter a scenario query", Toast.LENGTH_SHORT).show()
            return
        }

        // Hide result, show loader
        resultCard.visibility = View.GONE
        loadingCard.visibility = View.VISIBLE

        // Simulate AI API delay (1200ms)
        btnSimulate.postDelayed({
            if (!isAdded) return@postDelayed
            
            loadingCard.visibility = View.GONE
            
            val lower = userQuestion.lowercase()
            val phase = currentCyclePhase
            
            var riskLevel = "Moderate Risk"
            var response = ""
            val actions = mutableListOf<String>()

            when {
                lower.contains("5k") || lower.contains("train") || lower.contains("running") || lower.contains("workout") || lower.contains("exercise") || lower.contains("run") -> {
                    when {
                        phase.contains("Menstrual") -> {
                            riskLevel = "Moderate Risk"
                            response = "During your menstrual phase (Day $currentCycleDay), your energy and hormone levels are at their baseline. Starting intense 5K training right now might lead to fatigue and amplify cramps. We recommend starting slow with low-impact walk-runs."
                            actions.add("Limit running to 15-minute gentle walk-runs")
                            actions.add("Prioritize iron-rich meals to counteract blood loss")
                            actions.add("Schedule active recovery days in between runs")
                        }
                        phase.contains("Follicular") -> {
                            riskLevel = "Low Risk"
                            response = "Excellent timing! In your follicular phase (Day $currentCycleDay), rising estrogen levels boost your endurance, recovery, and pain tolerance. Your body is primed for building stamina and starting high-intensity runs."
                            actions.add("Follow a structured 5K training program")
                            actions.add("Focus on strength workouts to build leg stability")
                            actions.add("Keep hydrated and consume complex carbs before running")
                        }
                        phase.contains("Ovulation") -> {
                            riskLevel = "Low Risk"
                            response = "Peak performance zone! Around ovulation (Day $currentCycleDay), your strength and cardiorespiratory performance are at their peak due to high estrogen and testosterone. Push for speed, but warm up thoroughly as estrogen peaks make ligaments slightly lax."
                            actions.add("Incorporate short speed-intervals (HIIT)")
                            actions.add("Perform dynamic stretching to protect loose joints")
                            actions.add("Fuel with electrolyte-rich drinks post-run")
                        }
                        else -> { // Luteal
                            riskLevel = "High Risk"
                            response = "Progesterone is high during your luteal phase (Day $currentCycleDay), raising your body temperature and heart rate. High-intensity runs will feel significantly harder, and starting 5K training now could trigger high cortisol, worsen PMS, and cause early burnout."
                            actions.add("Stick to steady-state, low-intensity jogging or walking")
                            actions.add("Sleep at least 8 hours to offset high metabolic demand")
                            actions.add("Delay maximum effort runs until your period starts")
                        }
                    }
                }
                lower.contains("iron") || lower.contains("supplement") || lower.contains("vitamin") -> {
                    when {
                        phase.contains("Menstrual") -> {
                            riskLevel = "High Risk"
                            response = "Skipping iron supplements during your Menstrual Phase (Day $currentCycleDay) is highly discouraged. Due to active blood loss, your iron reserves deplete quickly, which can lead to fatigue, brain fog, and severe cramping."
                            actions.add("Take your iron supplement with Vitamin C for better absorption")
                            actions.add("Incorporate spinach, lentils, or red meat into meals")
                            actions.add("Avoid drinking tea or coffee close to supplement intake")
                        }
                        else -> {
                            riskLevel = "Low Risk"
                            response = "Skipping iron supplements for a day outside your period is generally low risk. However, maintaining consistent levels is still recommended to build up your baseline ferritin reserves."
                            actions.add("Resume your regular supplement routine tomorrow")
                            actions.add("Ensure adequate dietary iron through natural foods")
                            actions.add("Consult your doctor for customized dosage guidance")
                        }
                    }
                }
                lower.contains("fast") || lower.contains("fasting") || lower.contains("diet") -> {
                    when {
                        phase.contains("Luteal") || phase.contains("Menstrual") -> {
                            riskLevel = "High Risk"
                            response = "Intermittent fasting during your luteal or menstrual phases (Day $currentCycleDay) can severely disrupt cortisol and progesterone levels. Your body requires stable glucose to support hormone synthesis. Fasting now can lead to sleep disturbances, intense cravings, and mood crashes."
                            actions.add("Avoid fasting windows longer than 12 hours")
                            actions.add("Focus on warm, nutrient-dense meals containing healthy fats")
                            actions.add("Prioritize complex carbs to support blood sugar stability")
                        }
                        else -> { // Follicular / Ovulation
                            riskLevel = "Moderate Risk"
                            response = "Your body is more resilient to metabolic stress during the follicular phase (Day $currentCycleDay). If you choose to fast, keep it gentle. However, ensure you do not restrict essential nutrients needed for follicular development."
                            actions.add("Maintain a moderate fasting window (e.g., 12-14 hours)")
                            actions.add("Break your fast with high-protein and high-fiber foods")
                            actions.add("Stay hydrated with water, herbal teas, or electrolytes")
                        }
                    }
                }
                lower.contains("medication") || lower.contains("pill") || lower.contains("medicine") -> {
                    riskLevel = "Moderate Risk"
                    response = "Starting a new medication can influence your endocrine system. Depending on the drug type, it might shift your cycle length or phase onset. Always check with your doctor first."
                    actions.add("Discuss cycle-related side effects with your physician")
                    actions.add("Log your medication start date in your Sneh notes")
                    actions.add("Track changes in flow or symptoms over the next 30 days")
                }
                lower.contains("stress") -> {
                    when {
                        phase.contains("Luteal") -> {
                            riskLevel = "High Risk"
                            response = "High stress levels during your luteal phase (Day $currentCycleDay) severely suppress progesterone and amplify PMS. Cortisol competes with progesterone, leading to irritability, sleep issues, and headaches."
                            actions.add("Practice 10 minutes of deep box breathing daily")
                            actions.add("Reduce caffeine intake to prevent nervous system arousal")
                            actions.add("Engage in restorative yoga or nature walks")
                        }
                        phase.contains("Ovulation") -> {
                            riskLevel = "High Risk"
                            response = "High stress during ovulation can suppress the luteinizing hormone (LH) peak, which may delay or prevent egg release entirely, potentially disrupting your cycle length."
                            actions.add("Dedicate time for a relaxing warm bath or reading")
                            actions.add("Say no to extra tasks to lower cognitive load")
                            actions.add("Try a 5-minute guided meditation before bed")
                        }
                        else -> {
                            riskLevel = "Moderate Risk"
                            response = "While stress is always taxing, it is particularly disruptive during follicular growth. High cortisol can delay follicle maturation, pushing back your ovulation date."
                            actions.add("Incorporate gentle stretching routines")
                            actions.add("Track your heart rate variability (HRV) if possible")
                            actions.add("Ensure 7.5+ hours of quality sleep nightly")
                        }
                    }
                }
                lower.contains("sleep") || lower.contains("insomnia") || lower.contains("night") -> {
                    when {
                        phase.contains("Luteal") -> {
                            riskLevel = "High Risk"
                            response = "Sleep deprivation during your luteal phase (Day $currentCycleDay) acts as a physical stressor. Progesterone dips combined with poor sleep will exacerbate morning fatigue, brain fog, and fluid retention."
                            actions.add("Aim for 8 hours of sleep with a set bedtime routine")
                            actions.add("Avoid screens 1 hour before sleep to boost melatonin")
                            actions.add("Keep your bedroom cool to help lower body temp")
                        }
                        else -> {
                            riskLevel = "Moderate Risk"
                            response = "Rest is critical for cell repair across all phases. Restricting sleep to under 6 hours raises systemic inflammation and makes pain/cramps feel more intense during menstruation."
                            actions.add("Catch a 15-20 min afternoon nap if sleep was short")
                            actions.add("Stay extra hydrated and get natural morning sunlight")
                            actions.add("Avoid caffeine after 2:00 PM")
                        }
                    }
                }
                else -> {
                    riskLevel = "Moderate Risk"
                    response = "You are currently in your $phase (Day $currentCycleDay). Your body's nutritional and energy needs are cycle-dependent. Sneh AI recommends staying hydrated, eating balanced whole foods, and tuning your exercise intensity to your current phase requirements."
                    actions.add("Keep a daily hydration log (aim for 2.5L)")
                    actions.add("Perform a quick 5-minute body scan meditation")
                    actions.add("Get 15 minutes of light movement or stretching")
                }
            }

            // Save variables for potential Firestore save
            currentSimulationQuestion = userQuestion
            currentSimulationResponse = response
            currentSimulationRisk = riskLevel
            currentSimulationActions = actions

            // Bind values to UI
            tvUserQuestion.text = userQuestion
            tvAiResponse.text = response
            tvAction1.text = actions.getOrNull(0) ?: ""
            tvAction2.text = actions.getOrNull(1) ?: ""
            tvAction3.text = actions.getOrNull(2) ?: ""

            setImpactBadgeUI(riskLevel)

            resultCard.visibility = View.VISIBLE
            
            // Scroll to show results
            scrollView.post {
                scrollView.smoothScrollTo(0, resultCard.top)
            }
        }, 1200)
    }

    private fun setImpactBadgeUI(riskLevel: String) {
        val context = context ?: return
        tvImpactBadge.text = riskLevel

        when (riskLevel) {
            "Low Risk" -> {
                impactBadgeCard.setCardBackgroundColor(Color.parseColor("#E2F5ED"))
                tvImpactBadge.setTextColor(Color.parseColor("#1E6B52"))
            }
            "High Risk" -> {
                impactBadgeCard.setCardBackgroundColor(Color.parseColor("#FFE8D6"))
                tvImpactBadge.setTextColor(Color.parseColor("#C0531E"))
            }
            else -> {
                impactBadgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.blushPink))
                tvImpactBadge.setTextColor(ContextCompat.getColor(context, R.color.roseGold))
            }
        }
    }

    private fun saveSimulation() {
        val currentUser = auth.currentUser ?: return
        if (currentSimulationQuestion.isEmpty() || currentSimulationResponse.isEmpty()) return

        val simulationData = hashMapOf(
            "question" to currentSimulationQuestion,
            "response" to currentSimulationResponse,
            "riskLevel" to currentSimulationRisk,
            "actions" to currentSimulationActions,
            "cycleDay" to currentCycleDay,
            "cyclePhase" to currentCyclePhase,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("users").document(currentUser.uid)
            .collection("simulations")
            .add(simulationData)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Simulation saved successfully!", Toast.LENGTH_SHORT).show()
                    fetchPastSimulations(currentUser.uid)
                }
            }
    }

    private fun fetchPastSimulations(userId: String) {
        db.collection("users").document(userId)
            .collection("simulations")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                if (!isAdded) return@addOnSuccessListener
                llHistoryContainer.removeAllViews()
                
                if (docs.isEmpty) {
                    historyCard.visibility = View.GONE
                } else {
                    historyCard.visibility = View.VISIBLE
                    for (doc in docs) {
                        val question = doc.getString("question") ?: ""
                        val responseText = doc.getString("response") ?: ""
                        val riskLevel = doc.getString("riskLevel") ?: "Moderate Risk"
                        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        val cycleDay = doc.getLong("cycleDay")?.toInt() ?: 1
                        val actionsList = doc.get("actions") as? List<*> ?: listOf<String>()
                        
                        addHistoryItemView(question, responseText, riskLevel, timestamp, cycleDay, actionsList)
                    }
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    historyCard.visibility = View.GONE
                }
            }
    }

    private fun addHistoryItemView(
        question: String,
        responseText: String,
        riskLevel: String,
        timestamp: Long,
        cycleDay: Int,
        actionsList: List<*>
    ) {
        val context = context ?: return
        
        val itemLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_notif_item)
            setPadding(32, 28, 32, 28)
            isClickable = true
            isFocusable = true
        }

        val textContainer = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            orientation = LinearLayout.VERTICAL
        }

        val tvQuestion = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = question
            setTextColor(ContextCompat.getColor(context, R.color.calendarBrown))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12f)
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.poppins_regular)
        }

        val tvInfo = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 0)
            }
            
            val diffMs = System.currentTimeMillis() - timestamp
            val diffDays = (diffMs / (1000 * 60 * 60 * 24)).toInt()
            val timeStr = when {
                diffDays == 0 -> "Today"
                diffDays == 1 -> "Yesterday"
                diffDays in 2..7 -> "$diffDays days ago"
                else -> {
                    val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(timestamp))
                }
            }
            text = "$timeStr · Day $cycleDay"
            setTextColor(ContextCompat.getColor(context, R.color.textSecondary))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.poppins_regular)
        }

        textContainer.addView(tvQuestion)
        textContainer.addView(tvInfo)

        val badgeCard = com.google.android.material.card.MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(16, 0, 0, 0)
            }
            cardElevation = 0f
            radius = 16f
        }

        val tvBadge = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(16, 6, 16, 6)
            text = riskLevel
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.poppins_bold)
        }

        when (riskLevel) {
            "Low Risk" -> {
                badgeCard.setCardBackgroundColor(Color.parseColor("#E2F5ED"))
                tvBadge.setTextColor(Color.parseColor("#1E6B52"))
            }
            "High Risk" -> {
                badgeCard.setCardBackgroundColor(Color.parseColor("#FFE8D6"))
                tvBadge.setTextColor(Color.parseColor("#C0531E"))
            }
            else -> {
                badgeCard.setCardBackgroundColor(ContextCompat.getColor(context, R.color.blushPink))
                tvBadge.setTextColor(ContextCompat.getColor(context, R.color.roseGold))
            }
        }

        badgeCard.addView(tvBadge)

        itemLayout.addView(textContainer)
        itemLayout.addView(badgeCard)

        itemLayout.setOnClickListener {
            etWhatIfInput.setText(question)
            tvUserQuestion.text = question
            tvAiResponse.text = responseText
            
            tvAction1.text = actionsList.getOrNull(0)?.toString() ?: ""
            tvAction2.text = actionsList.getOrNull(1)?.toString() ?: ""
            tvAction3.text = actionsList.getOrNull(2)?.toString() ?: ""
            
            setImpactBadgeUI(riskLevel)
            
            resultCard.visibility = View.VISIBLE
            
            scrollView.post {
                scrollView.smoothScrollTo(0, resultCard.top)
            }
        }

        llHistoryContainer.addView(itemLayout)
    }

    private fun clearHistory() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid)
            .collection("simulations")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val batch = db.batch()
                for (doc in querySnapshot) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    if (isAdded) {
                        Toast.makeText(context, "History cleared!", Toast.LENGTH_SHORT).show()
                        llHistoryContainer.removeAllViews()
                        historyCard.visibility = View.GONE
                    }
                }
            }
    }
}
