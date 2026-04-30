package com.sneh.app.onboarding.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.sneh.app.R
import com.sneh.app.onboarding.OnboardingViewModel

class Step2CycleFragment : Fragment() {

    private lateinit var vm: OnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        val view = inflater.inflate(R.layout.fragment_step2, container, false)

        val cycleLength = view.findViewById<EditText>(R.id.cycleLength)
        val periodLength = view.findViewById<EditText>(R.id.periodLength)
        val lastDate = view.findViewById<EditText>(R.id.lastPeriodDate)
        val regularity = view.findViewById<Spinner>(R.id.regularity)
        val nextBtn = view.findViewById<Button>(R.id.nextBtn)

        nextBtn.setOnClickListener {

            vm.cycleLength = cycleLength.text.toString().toIntOrNull() ?: 28
            vm.periodLength = periodLength.text.toString().toIntOrNull() ?: 5
            vm.lastPeriodDate = lastDate.text.toString()
            vm.regularity = regularity.selectedItem?.toString() ?: ""

            parentFragmentManager.beginTransaction()
                .replace(R.id.container, Step3HealthFragment())
                .commit()
        }

        return view
    }
}