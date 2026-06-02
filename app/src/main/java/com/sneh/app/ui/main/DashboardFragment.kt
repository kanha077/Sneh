package com.sneh.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import com.sneh.app.auth.LoginActivity
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        val logo = view.findViewById<ImageView>(R.id.logo)
        val logoutBtn = view.findViewById<ImageButton>(R.id.logoutBtn)
        
        val moduleCycle = view.findViewById<View>(R.id.moduleCycle)
        val moduleMind = view.findViewById<View>(R.id.moduleMind)
        val modulePhysical = view.findViewById<View>(R.id.modulePhysical)
        val moduleNutrition = view.findViewById<View>(R.id.moduleNutrition)
        val moduleFertility = view.findViewById<View>(R.id.moduleFertility)
        val moduleExpert = view.findViewById<View>(R.id.moduleExpert)

        // 📅 Navigation Logic
        moduleCycle.setOnClickListener { navigateTo(CalendarFragment()) }
        
        moduleMind.setOnClickListener { navigateTo(PlaceholderFragment.newInstance("Mind Wellness")) }
        modulePhysical.setOnClickListener { navigateTo(PlaceholderFragment.newInstance("Physical Fitness")) }
        moduleNutrition.setOnClickListener { navigateTo(PlaceholderFragment.newInstance("Nutrition")) }
        moduleFertility.setOnClickListener { navigateTo(PlaceholderFragment.newInstance("Fertility Plan")) }
        moduleExpert.setOnClickListener { navigateTo(PlaceholderFragment.newInstance("Expert & Community")) }

        // 🚪 Logout
        logoutBtn.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // 🔐 Redirect if not logged in
        if (auth.currentUser == null) {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        return view
    }

    private fun navigateTo(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // 🌸 PHASE CALCULATION
    private fun getCycleDayAndPhase(lastPeriod: String, cycleLength: Int): Pair<Int, String> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val last = sdf.parse(lastPeriod) ?: return 1 to "Unknown"

            val today = Date()
            val days = ((today.time - last.time) / (1000 * 60 * 60 * 24)).toInt()
            val cycleDay = (days % cycleLength) + 1

            val phase = when (cycleDay) {
                in 1..5 -> "Menstrual Phase 🩸"
                in 6..13 -> "Follicular Phase 🌱"
                in 14..16 -> "Ovulation Phase 🌼"
                else -> "Luteal Phase 🌙"
            }

            cycleDay to phase

        } catch (e: Exception) {
            1 to "Unknown"
        }
    }

    // 💡 RECOMMENDATIONS
    private fun getRecommendation(phase: String): String {
        return when {
            phase.contains("Menstrual") -> "Rest well, hydrate, and use warm foods."
            phase.contains("Follicular") -> "Great time for workouts & new plans."
            phase.contains("Ovulation") -> "Peak energy—be social & productive."
            phase.contains("Luteal") -> "Slow down, light exercise, self-care."
            else -> "Track regularly for insights."
        }
    }

    // 📅 CALENDAR SETUP
    private fun setupCalendar(
        grid: GridLayout,
        lastPeriod: String,
        cycleLength: Int
    ) {

        grid.removeAllViews()

        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val last = sdf.parse(lastPeriod)

        for (day in 1..daysInMonth) {

            val tv = TextView(requireContext())
            tv.text = day.toString()
            tv.gravity = Gravity.CENTER
            tv.setPadding(20, 20, 20, 20)

            val cellDate = Calendar.getInstance()
            cellDate.set(Calendar.DAY_OF_MONTH, day)

            val diff = if (last != null) {
                ((cellDate.time.time - last.time) / (1000 * 60 * 60 * 24)).toInt()
            } else 0

            val cycleDay = (diff % cycleLength + cycleLength) % cycleLength + 1

            when (cycleDay) {
                in 1..5 -> tv.setBackgroundResource(R.drawable.bg_period_day)
                in 14..16 -> tv.setBackgroundResource(R.drawable.bg_ovulation_day)
            }

            if (day == today.get(Calendar.DAY_OF_MONTH)) {
                tv.setBackgroundResource(R.drawable.bg_today)
            }

            grid.addView(tv)
        }
    }
}