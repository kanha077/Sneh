package com.sneh.app.onboarding.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
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

    private var selectedActivity = "Moderate"
    private var selectedDiet = "Vegetarian"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_step3, container, false)

        val stressSeekBar = view.findViewById<SeekBar>(R.id.stressLevel)
        val finishBtn = view.findViewById<Button>(R.id.finishBtn)

        val chipActSedentary = view.findViewById<TextView>(R.id.chipActSedentary)
        val chipActModerate = view.findViewById<TextView>(R.id.chipActModerate)
        val chipActActive = view.findViewById<TextView>(R.id.chipActActive)

        val activityChips = mapOf(
            "Sedentary" to chipActSedentary,
            "Moderate" to chipActModerate,
            "Active" to chipActActive
        )

        val chipDietVeg = view.findViewById<TextView>(R.id.chipDietVeg)
        val chipDietNonVeg = view.findViewById<TextView>(R.id.chipDietNonVeg)
        val chipDietVegan = view.findViewById<TextView>(R.id.chipDietVegan)

        val dietChips = mapOf(
            "Vegetarian" to chipDietVeg,
            "Non-veg" to chipDietNonVeg,
            "Vegan" to chipDietVegan
        )

        fun updateActivityUI() {
            activityChips.forEach { (level, textView) ->
                if (level == selectedActivity) {
                    textView.setBackgroundResource(R.drawable.bg_task_done)
                    textView.setTextColor(Color.WHITE)
                } else {
                    textView.setBackgroundResource(R.drawable.bg_task_pending)
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendarBrown))
                }
            }
        }

        fun updateDietUI() {
            dietChips.forEach { (pref, textView) ->
                if (pref == selectedDiet) {
                    textView.setBackgroundResource(R.drawable.bg_task_done)
                    textView.setTextColor(Color.WHITE)
                } else {
                    textView.setBackgroundResource(R.drawable.bg_task_pending)
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendarBrown))
                }
            }
        }

        activityChips.forEach { (level, textView) ->
            textView.setOnClickListener {
                selectedActivity = level
                updateActivityUI()
            }
        }

        dietChips.forEach { (pref, textView) ->
            textView.setOnClickListener {
                selectedDiet = pref
                updateDietUI()
            }
        }

        updateActivityUI()
        updateDietUI()

        finishBtn.setOnClickListener {

            // 1. Guard: user must be logged in
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e("Step3", "No authenticated user found — cannot save profile")
                Toast.makeText(context, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 2. Map stress slider to string
            val stress = stressSeekBar.progress
            val stressStr = when {
                stress <= 33 -> "Low"
                stress <= 66 -> "Moderate"
                else -> "High"
            }

            vm.activityLevel = selectedActivity
            vm.stressLevel = stressStr
            vm.sleepQuality = "Moderate"
            vm.exerciseFrequency = selectedActivity

            Log.d("Step3", "Saving profile for userId=$userId")
            Log.d("Step3", "ViewModel — name=${vm.name}, age=${vm.age}, " +
                    "cycleLength=${vm.cycleLength}, periodLength=${vm.periodLength}, " +
                    "lastPeriodDate=${vm.lastPeriodDate}, goal=${vm.goal}, " +
                    "symptoms=${vm.symptoms}, activityLevel=${vm.activityLevel}, " +
                    "diet=$selectedDiet, stress=$stressStr")

            // 3. Fetch the name saved during signup (stored in Firestore by SignupActivity)
            //    so we don't depend on vm.name being set during onboarding navigation
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    val profileMap = doc.get("profile") as? Map<*, *>
                    val nameFromFirestore = profileMap?.get("name") as? String
                        ?: vm.name.ifEmpty { "User" }

                    val data = hashMapOf(
                        "profile" to mapOf(
                            "name" to nameFromFirestore,
                            "age" to vm.age,
                            "activityLevel" to vm.activityLevel
                        ),
                        "cycle" to mapOf(
                            "cycleLength" to vm.cycleLength,
                            "periodLength" to vm.periodLength,
                            "lastPeriodDate" to vm.lastPeriodDate,
                            "regularity" to "Regular"
                        ),
                        "health" to mapOf(
                            "symptoms" to vm.symptoms
                        ),
                        "lifestyle" to mapOf(
                            "sleep" to vm.sleepQuality,
                            "stress" to vm.stressLevel,
                            "exercise" to vm.exerciseFrequency,
                            "diet" to selectedDiet
                        ),
                        "goal" to vm.goal,
                        "onboarding" to mapOf("completed" to true)
                    )

                    db.collection("users").document(userId)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("Step3", "Profile saved successfully")

                            val prefs = requireContext()
                                .getSharedPreferences("app", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("onboarded", true).apply()

                            val intent = Intent(requireContext(), MainActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        .addOnFailureListener { e ->
                            Log.e("Step3", "Firestore write failed: ${e.message}", e)
                            Toast.makeText(
                                context,
                                "Failed to save profile: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Step3", "Could not read existing user doc: ${e.message}", e)
                    // Still attempt to write with whatever data we have
                    val data = hashMapOf(
                        "profile" to mapOf(
                            "name" to vm.name.ifEmpty { "User" },
                            "age" to vm.age,
                            "activityLevel" to vm.activityLevel
                        ),
                        "cycle" to mapOf(
                            "cycleLength" to vm.cycleLength,
                            "periodLength" to vm.periodLength,
                            "lastPeriodDate" to vm.lastPeriodDate,
                            "regularity" to "Regular"
                        ),
                        "health" to mapOf(
                            "symptoms" to vm.symptoms
                        ),
                        "lifestyle" to mapOf(
                            "sleep" to vm.sleepQuality,
                            "stress" to vm.stressLevel,
                            "exercise" to vm.exerciseFrequency,
                            "diet" to selectedDiet
                        ),
                        "goal" to vm.goal,
                        "onboarding" to mapOf("completed" to true)
                    )

                    db.collection("users").document(userId)
                        .set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("Step3", "Profile saved (fallback path) successfully")
                            val prefs = requireContext()
                                .getSharedPreferences("app", Context.MODE_PRIVATE)
                            prefs.edit().putBoolean("onboarded", true).apply()
                            val intent = Intent(requireContext(), MainActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            requireActivity().finish()
                        }
                        .addOnFailureListener { e2 ->
                            Log.e("Step3", "Firestore fallback write also failed: ${e2.message}", e2)
                            Toast.makeText(
                                context,
                                "Failed to save profile: ${e2.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
        }

        return view
    }
}