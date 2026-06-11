package com.sneh.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.sneh.app.R

class MainActivity : AppCompatActivity() {

    private lateinit var tabHome: LinearLayout
    private lateinit var tabCycle: LinearLayout
    private lateinit var tabMind: LinearLayout
    private lateinit var tabNutrition: LinearLayout
    private lateinit var tabMe: LinearLayout

    private lateinit var txtHome: TextView
    private lateinit var txtCycle: TextView
    private lateinit var txtMind: TextView
    private lateinit var txtNutrition: TextView
    private lateinit var txtMe: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI
        tabHome = findViewById(R.id.tabHome)
        tabCycle = findViewById(R.id.tabCycle)
        tabMind = findViewById(R.id.tabMind)
        tabNutrition = findViewById(R.id.tabNutrition)
        tabMe = findViewById(R.id.tabMe)

        txtHome = findViewById(R.id.tabHomeText)
        txtCycle = findViewById(R.id.tabCycleText)
        txtMind = findViewById(R.id.tabMindText)
        txtNutrition = findViewById(R.id.tabNutritionText)
        txtMe = findViewById(R.id.tabMeText)

        // Click listeners
        tabHome.setOnClickListener { switchFragment(DashboardFragment(), txtHome) }
        tabCycle.setOnClickListener { switchFragment(CalendarFragment(), txtCycle) }
        tabMind.setOnClickListener { switchFragment(MindWellnessFragment(), txtMind) }
        tabNutrition.setOnClickListener { switchFragment(NutritionFragment(), txtNutrition) }
        tabMe.setOnClickListener { switchFragment(PhysicianReportFragment(), txtMe) }

        // Load Dashboard by default
        if (savedInstanceState == null) {
            switchFragment(DashboardFragment(), txtHome)
        }
    }

    fun navigateToTab(tabId: Int) {
        when (tabId) {
            R.id.tabHome -> switchFragment(DashboardFragment(), txtHome)
            R.id.tabCycle -> switchFragment(CalendarFragment(), txtCycle)
            R.id.tabMind -> switchFragment(MindWellnessFragment(), txtMind)
            R.id.tabNutrition -> switchFragment(NutritionFragment(), txtNutrition)
            R.id.tabMe -> switchFragment(PhysicianReportFragment(), txtMe)
        }
    }

    private fun switchFragment(fragment: Fragment, selectedTextView: TextView) {
        // Reset all text colors
        val subtleColor = ContextCompat.getColor(this, R.color.snehSubtle)
        txtHome.setTextColor(subtleColor)
        txtCycle.setTextColor(subtleColor)
        txtMind.setTextColor(subtleColor)
        txtNutrition.setTextColor(subtleColor)
        txtMe.setTextColor(subtleColor)

        // Set selected text color
        selectedTextView.setTextColor(ContextCompat.getColor(this, R.color.snehRose))

        // Replace fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}