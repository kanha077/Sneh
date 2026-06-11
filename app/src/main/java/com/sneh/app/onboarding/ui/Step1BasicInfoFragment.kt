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

class Step1BasicInfoFragment : Fragment() {

    private lateinit var vm: OnboardingViewModel
    private val selectedConditions = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_step1, container, false)

        val ageEdit = view.findViewById<EditText>(R.id.age)
        val firstPeriodAgeEdit = view.findViewById<EditText>(R.id.firstPeriodAge)
        val cycleLengthEdit = view.findViewById<EditText>(R.id.cycleLength)
        val nextBtn = view.findViewById<Button>(R.id.nextBtn)

        val chipPCOS = view.findViewById<TextView>(R.id.chipPCOS)
        val chipEndo = view.findViewById<TextView>(R.id.chipEndo)
        val chipThyroid = view.findViewById<TextView>(R.id.chipThyroid)
        val chipNone = view.findViewById<TextView>(R.id.chipNone)

        val chips = mapOf(
            "PCOS" to chipPCOS,
            "Endometriosis" to chipEndo,
            "Thyroid" to chipThyroid,
            "None" to chipNone
        )

        fun updateChipUI(key: String, chip: TextView) {
            val isSelected = selectedConditions.contains(key)
            if (isSelected) {
                chip.setBackgroundResource(R.drawable.bg_task_done)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.bg_task_pending)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendarBrown))
            }
        }

        fun toggleCondition(key: String) {
            if (key == "None") {
                selectedConditions.clear()
                selectedConditions.add("None")
            } else {
                selectedConditions.remove("None")
                if (selectedConditions.contains(key)) {
                    selectedConditions.remove(key)
                } else {
                    selectedConditions.add(key)
                }
                if (selectedConditions.isEmpty()) {
                    selectedConditions.add("None")
                }
            }
            chips.forEach { (k, v) -> updateChipUI(k, v) }
        }

        chipPCOS.setOnClickListener { toggleCondition("PCOS") }
        chipEndo.setOnClickListener { toggleCondition("Endometriosis") }
        chipThyroid.setOnClickListener { toggleCondition("Thyroid") }
        chipNone.setOnClickListener { toggleCondition("None") }

        // Set default selection
        toggleCondition("None")

        nextBtn.setOnClickListener {
            val age = ageEdit.text.toString().toIntOrNull() ?: 0
            val cycleLength = cycleLengthEdit.text.toString().toIntOrNull() ?: 28
            
            if (age <= 0) {
                Toast.makeText(context, "Please enter your age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            vm.age = age
            vm.cycleLength = cycleLength
            
            // Storing conditions into symptoms list for summary purposes
            vm.symptoms.clear()
            vm.symptoms.addAll(selectedConditions.filter { it != "None" })

            parentFragmentManager.beginTransaction()
                .replace(R.id.container, Step2CycleFragment())
                .commit()
        }

        return view
    }
}
