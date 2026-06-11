package com.sneh.app.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import com.sneh.app.core.CycleUtils

class FertilityPlanFragment : Fragment() {

    private lateinit var txtFertileChance: TextView
    private lateinit var txtDaysToOvulation: TextView
    private lateinit var txtFertilityTip: TextView
    private lateinit var fertilityCountdownProgress: ProgressBar

    private lateinit var imgFolicCheck: ImageView
    private lateinit var imgBBTCheck: ImageView

    private var folicChecked = false
    private var bbtChecked = false

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var cycleLength = 28
    private var lastPeriodDate = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_fertility_plan, container, false)

        txtFertileChance = view.findViewById(R.id.txtFertileChance)
        txtDaysToOvulation = view.findViewById(R.id.txtDaysToOvulation)
        txtFertilityTip = view.findViewById(R.id.txtFertilityTip)
        fertilityCountdownProgress = view.findViewById(R.id.fertilityCountdownProgress)

        imgFolicCheck = view.findViewById(R.id.imgFolicCheck)
        imgBBTCheck = view.findViewById(R.id.imgBBTCheck)

        view.findViewById<View>(R.id.taskFolic).setOnClickListener {
            folicChecked = !folicChecked
            imgFolicCheck.setImageResource(if (folicChecked) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)
        }

        view.findViewById<View>(R.id.taskBBT).setOnClickListener {
            bbtChecked = !bbtChecked
            imgBBTCheck.setImageResource(if (bbtChecked) R.drawable.ic_check_circle else R.drawable.ic_circle_outline)
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

                    updateFertilityUI()
                }
            }
    }

    private fun updateFertilityUI() {
        if (lastPeriodDate.isEmpty()) {
            txtFertileChance.text = "No Data"
            txtDaysToOvulation.text = "-"
            txtFertilityTip.text = "Please set last period date in calendar to enable tracking."
            return
        }

        val (day, phase) = CycleUtils.getCycleDayAndPhase(lastPeriodDate, cycleLength)
        val cycleDay = (day % cycleLength)

        fertilityCountdownProgress.max = cycleLength
        fertilityCountdownProgress.progress = cycleDay

        when {
            cycleDay in 1..10 -> {
                txtFertileChance.text = "Low Conception Chance"
                val days = 14 - cycleDay
                txtDaysToOvulation.text = days.toString()
                txtFertilityTip.text = "Focus on base level hydration and regular workouts. Peak window starts in $days days."
            }
            cycleDay in 11..13 -> {
                txtFertileChance.text = "High Conception Chance 🔥"
                val days = 14 - cycleDay
                txtDaysToOvulation.text = days.toString()
                txtFertilityTip.text = "Estrogen levels are high. Peak fertility is approaching in $days days!"
            }
            cycleDay == 14 -> {
                txtFertileChance.text = "Peak Fertility Day 🌟"
                txtDaysToOvulation.text = "0"
                txtFertilityTip.text = "Ovulation Day! Today presents the highest odds of conception this cycle."
            }
            cycleDay in 15..17 -> {
                txtFertileChance.text = "High Conception Chance"
                txtDaysToOvulation.text = "Post"
                txtFertilityTip.text = "Luteinizing hormone is declining. Transitioning into the luteal phase shortly."
            }
            else -> {
                txtFertileChance.text = "Low Conception Chance"
                txtDaysToOvulation.text = "-"
                txtFertilityTip.text = "Entering luteal phase. Progesterone rising. Conception odds are minimal."
            }
        }
    }
}
