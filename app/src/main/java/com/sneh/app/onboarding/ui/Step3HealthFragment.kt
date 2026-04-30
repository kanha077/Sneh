package com.sneh.app.onboarding.ui

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sneh.app.R
import com.sneh.app.onboarding.OnboardingViewModel
import com.sneh.app.ui.main.MainActivity

class Step3HealthFragment : Fragment() {

    private lateinit var vm: OnboardingViewModel
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        val view = inflater.inflate(R.layout.fragment_step3, container, false)

        val finishBtn = view.findViewById<Button>(R.id.finishBtn)

        val cramps = view.findViewById<CheckBox>(R.id.cramps)
        val mood = view.findViewById<CheckBox>(R.id.moodSwings)
        val fatigue = view.findViewById<CheckBox>(R.id.fatigue)

        val sleep = view.findViewById<Spinner>(R.id.sleepQuality)
        val stress = view.findViewById<Spinner>(R.id.stressLevel)
        val exercise = view.findViewById<Spinner>(R.id.exerciseFrequency)
        val goalGroup = view.findViewById<RadioGroup>(R.id.goalGroup)

        finishBtn.setOnClickListener {

            vm.symptoms.clear()
            if (cramps.isChecked) vm.symptoms.add("Cramps")
            if (mood.isChecked) vm.symptoms.add("Mood Swings")
            if (fatigue.isChecked) vm.symptoms.add("Fatigue")

            vm.sleepQuality = sleep.selectedItem?.toString() ?: ""
            vm.stressLevel = stress.selectedItem?.toString() ?: ""
            vm.exerciseFrequency = exercise.selectedItem?.toString() ?: ""

            val selectedGoalId = goalGroup.checkedRadioButtonId
            if (selectedGoalId != -1) {
                val selected = view.findViewById<RadioButton>(selectedGoalId)
                vm.goal = selected.text.toString()
            }

            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            val data = hashMapOf(
                "profile" to mapOf(
                    "name" to vm.name,
                    "age" to vm.age,
                    "activityLevel" to vm.activityLevel
                ),
                "cycle" to mapOf(
                    "cycleLength" to vm.cycleLength,
                    "periodLength" to vm.periodLength,
                    "lastPeriodDate" to vm.lastPeriodDate,
                    "regularity" to vm.regularity
                ),
                "health" to mapOf(
                    "symptoms" to vm.symptoms
                ),
                "lifestyle" to mapOf(
                    "sleep" to vm.sleepQuality,
                    "stress" to vm.stressLevel,
                    "exercise" to vm.exerciseFrequency
                ),
                "goal" to vm.goal,
                "onboarding" to mapOf("completed" to true)
            )

            db.collection("users")
                .document(userId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener {

                    // ✅ SAVE LOCALLY (MOST IMPORTANT FIX)
                    val prefs = requireContext().getSharedPreferences("app", android.content.Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("onboarded", true).apply()

                    // 🚀 GO TO MAIN
                    startActivity(Intent(requireContext(), MainActivity::class.java))
                    requireActivity().finish()
                }
        }

        return view
    }
}