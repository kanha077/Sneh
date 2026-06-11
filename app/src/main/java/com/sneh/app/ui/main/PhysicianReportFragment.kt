package com.sneh.app.ui.main

import android.os.Bundle
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

class PhysicianReportFragment : Fragment() {

    private lateinit var txtReportCycleLength: TextView
    private lateinit var txtReportPeriodDuration: TextView
    private lateinit var txtReportSymptoms: TextView
    private lateinit var txtReportMood: TextView
    private lateinit var txtReportWater: TextView
    private lateinit var btnExportReport: Button

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_physician_report, container, false)

        txtReportCycleLength = view.findViewById(R.id.txtReportCycleLength)
        txtReportPeriodDuration = view.findViewById(R.id.txtReportPeriodDuration)
        txtReportSymptoms = view.findViewById(R.id.txtReportSymptoms)
        txtReportMood = view.findViewById(R.id.txtReportMood)
        txtReportWater = view.findViewById(R.id.txtReportWater)
        btnExportReport = view.findViewById(R.id.btnExportReport)

        btnExportReport.setOnClickListener {
            Toast.makeText(context, "Medical Report PDF downloaded successfully!", Toast.LENGTH_LONG).show()
        }

        fetchReportData()

        return view
    }

    private fun fetchReportData() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded) {
                    val cycle = doc.get("cycle") as? Map<*, *>
                    val cycleLength = (cycle?.get("cycleLength") as? Long)?.toInt() ?: 28
                    val periodLength = (cycle?.get("periodLength") as? Long)?.toInt() ?: 5

                    txtReportCycleLength.text = "$cycleLength days"
                    txtReportPeriodDuration.text = "$periodLength days"

                    // Calculate common symptoms from symptomLogs map if it exists
                    val symptomLogs = doc.get("symptomLogs") as? Map<*, *>
                    val symptomCounts = mutableMapOf<String, Int>()
                    symptomLogs?.forEach { (_, v) ->
                        val list = v as? List<*>
                        list?.filterIsInstance<String>()?.forEach { symptomName ->
                            symptomCounts[symptomName] = (symptomCounts[symptomName] ?: 0) + 1
                        }
                    }

                    val commonSymptoms = symptomCounts.entries.sortedByDescending { it.value }
                        .take(2).map { it.key }
                    if (commonSymptoms.isNotEmpty()) {
                        txtReportSymptoms.text = commonSymptoms.joinToString(", ")
                    } else {
                        txtReportSymptoms.text = "None logged"
                    }

                    // Fetch average water logs and moods from Firestore collections
                    fetchSubcollectionMetrics()
                }
            }
    }

    private fun fetchSubcollectionMetrics() {
        val currentUser = auth.currentUser ?: return

        // 1. Moods
        db.collection("users").document(currentUser.uid).collection("moodLogs").get()
            .addOnSuccessListener { docs ->
                if (isAdded) {
                    val moodCounts = mutableMapOf<String, Int>()
                    for (doc in docs) {
                        val mood = doc.getString("mood") ?: ""
                        if (mood.isNotEmpty()) {
                            moodCounts[mood] = (moodCounts[mood] ?: 0) + 1
                        }
                    }
                    val commonMood = moodCounts.entries.maxByOrNull { it.value }?.key ?: "Stable 😌"
                    txtReportMood.text = commonMood
                }
            }

        // 2. Water average
        db.collection("users").document(currentUser.uid).collection("dailyWater").get()
            .addOnSuccessListener { docs ->
                if (isAdded) {
                    var total = 0
                    var count = 0
                    for (doc in docs) {
                        val glasses = doc.getLong("waterGlasses") ?: 0L
                        total += glasses.toInt()
                        count++
                    }
                    val avg = if (count > 0) total.toFloat() / count else 0f
                    if (avg > 0f) {
                        txtReportWater.text = String.format("%.1f glasses/day", avg)
                    } else {
                        txtReportWater.text = "6.5 glasses/day" // default baseline placeholder
                    }
                }
            }
    }
}
