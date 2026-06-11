package com.sneh.app.core

import java.text.SimpleDateFormat
import java.util.*

object CycleUtils {

    // Returns Pair of (CycleDay, PhaseName)
    fun getCycleDayAndPhase(lastPeriod: String, cycleLength: Int): Pair<Int, String> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val last = sdf.parse(lastPeriod) ?: return 1 to "Unknown"

            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val lastCal = Calendar.getInstance().apply {
                time = last
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMs = todayCal.timeInMillis - lastCal.timeInMillis
            val daysElapsed = (diffMs / (1000 * 60 * 60 * 24)).toInt()

            if (daysElapsed < 0) {
                return 1 to "Future Date"
            }

            if (daysElapsed >= cycleLength) {
                val cycleDay = daysElapsed + 1
                return cycleDay to "Overdue ⚠️"
            }

            val cycleDay = daysElapsed + 1
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
}
