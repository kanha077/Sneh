package com.sneh.app.onboarding.ui

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.sneh.app.R
import com.sneh.app.onboarding.OnboardingViewModel
import java.text.SimpleDateFormat
import java.util.*

class Step2CycleFragment : Fragment() {

    private lateinit var vm: OnboardingViewModel
    private var selectedDuration = 5
    private var selectedGoal = "Conceive"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_step2, container, false)

        val dateEdit = view.findViewById<EditText>(R.id.lastPeriodDate)
        val nextBtn = view.findViewById<Button>(R.id.nextBtn)

        val chipD3 = view.findViewById<TextView>(R.id.chipDur3)
        val chipD4 = view.findViewById<TextView>(R.id.chipDur4)
        val chipD5 = view.findViewById<TextView>(R.id.chipDur5)
        val chipD6 = view.findViewById<TextView>(R.id.chipDur6)
        val chipD7 = view.findViewById<TextView>(R.id.chipDur7)

        val durationChips = mapOf(
            3 to chipD3,
            4 to chipD4,
            5 to chipD5,
            6 to chipD6,
            7 to chipD7
        )

        val chipGoalTrack = view.findViewById<TextView>(R.id.chipGoalTrack)
        val chipGoalConceive = view.findViewById<TextView>(R.id.chipGoalConceive)
        val chipGoalAvoid = view.findViewById<TextView>(R.id.chipGoalAvoid)

        val goalChips = mapOf(
            "Track health" to chipGoalTrack,
            "Conceive" to chipGoalConceive,
            "Avoid pregnancy" to chipGoalAvoid
        )

        fun updateDurationUI() {
            durationChips.forEach { (days, textView) ->
                if (days == selectedDuration) {
                    textView.setBackgroundResource(R.drawable.bg_task_done)
                    textView.setTextColor(Color.WHITE)
                } else {
                    textView.setBackgroundResource(R.drawable.bg_task_pending)
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendarBrown))
                }
            }
        }

        fun updateGoalUI() {
            goalChips.forEach { (goalText, textView) ->
                if (goalText == selectedGoal) {
                    textView.setBackgroundResource(R.drawable.bg_task_done)
                    textView.setTextColor(Color.WHITE)
                } else {
                    textView.setBackgroundResource(R.drawable.bg_task_pending)
                    textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendarBrown))
                }
            }
        }

        durationChips.forEach { (days, textView) ->
            textView.setOnClickListener {
                selectedDuration = days
                updateDurationUI()
            }
        }

        goalChips.forEach { (goalText, textView) ->
            textView.setOnClickListener {
                selectedGoal = goalText
                updateGoalUI()
            }
        }

        // Set Date Picker
        dateEdit.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePickerDialog = android.app.DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance()
                    selectedCal.set(Calendar.YEAR, year)
                    selectedCal.set(Calendar.MONTH, month)
                    selectedCal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val sdfDisplay = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    
                    dateEdit.setText(sdfDisplay.format(selectedCal.time))
                    vm.lastPeriodDate = sdf.format(selectedCal.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // Apply defaults
        updateDurationUI()
        updateGoalUI()

        nextBtn.setOnClickListener {
            if (vm.lastPeriodDate.isEmpty()) {
                Toast.makeText(context, "Please select your last period start date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            vm.periodLength = selectedDuration
            vm.goal = selectedGoal

            parentFragmentManager.beginTransaction()
                .replace(R.id.container, Step3HealthFragment())
                .commit()
        }

        return view
    }
}
