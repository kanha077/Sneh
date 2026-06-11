package com.sneh.app.onboarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.sneh.app.R
import com.sneh.app.onboarding.ui.Step1BasicInfoFragment

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, Step1BasicInfoFragment())
                .commit()
        }
    }
}
