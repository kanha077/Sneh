package com.sneh.app.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sneh.app.R

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ✅ LOAD DASHBOARD
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, DashboardFragment())
                .commit()
        }
    }
}