package com.sneh.app.onboarding

import androidx.lifecycle.ViewModel

class OnboardingViewModel : ViewModel() {

    // Step 1
    var name = ""
    var age = 0
    var activityLevel = ""

    // Step 2
    var cycleLength = 28
    var periodLength = 5
    var lastPeriodDate = ""
    var regularity = ""

    // Step 3
    var symptoms = mutableListOf<String>()
    var sleepQuality = ""
    var stressLevel = ""
    var exerciseFrequency = ""
    var goal = ""
}