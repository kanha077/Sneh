package com.sneh.app.ui.main

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.sneh.app.R

class PlaceholderFragment : Fragment() {

    companion object {
        fun newInstance(title: String): PlaceholderFragment {
            val fragment = PlaceholderFragment()
            val args = Bundle()
            args.putString("title", title)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_step1, container, false) // Reuse a layout for simple placeholder
        val titleText = view.findViewById<TextView>(R.id.name).apply {
            hint = ""
            setText(arguments?.getString("title") ?: "Coming Soon")
            isEnabled = false
        }
        val nextBtn = view.findViewById<Button>(R.id.nextBtn).apply {
            text = "Back to Dashboard"
            setOnClickListener { parentFragmentManager.popBackStack() }
        }
        return view
    }
}