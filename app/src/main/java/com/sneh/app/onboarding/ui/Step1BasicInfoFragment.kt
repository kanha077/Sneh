package com.sneh.app.onboarding.ui

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.sneh.app.R
import com.sneh.app.onboarding.OnboardingViewModel

class Step1BasicInfoFragment : Fragment() {

    private lateinit var vm: OnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm = ViewModelProvider(requireActivity())[OnboardingViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {

        val view = inflater.inflate(R.layout.fragment_step1, container, false)

        val name = view.findViewById<EditText>(R.id.name)
        val age = view.findViewById<EditText>(R.id.age)
        val activity = view.findViewById<Spinner>(R.id.activityLevel)
        val nextBtn = view.findViewById<Button>(R.id.nextBtn)

        nextBtn.setOnClickListener {

            vm.name = name.text.toString()
            vm.age = age.text.toString().toIntOrNull() ?: 0
            vm.activityLevel = activity.selectedItem?.toString() ?: ""

            parentFragmentManager.beginTransaction()
                .replace(R.id.container, Step2CycleFragment())
                .commit()
        }

        return view
    }
}