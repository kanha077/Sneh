package com.sneh.app.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneh.app.R
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var txtMonthYear: TextView
    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var txtSelectedDay: TextView
    private lateinit var btnSetPeriodStart: Button
    private lateinit var btnSaveSymptoms: Button

    // Symptoms views
    private lateinit var chipCramps: TextView
    private lateinit var chipHeadache: TextView
    private lateinit var chipFatigue: TextView
    private lateinit var chipMood: TextView
    private lateinit var chipBloating: TextView

    private val selectedSymptoms = mutableSetOf<String>()
    private lateinit var calendar: Calendar
    private lateinit var selectedDate: Calendar

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var cycleLength = 28
    private var lastPeriodDate = ""
    private val loggedSymptomsMap = mutableMapOf<String, List<String>>() // Key: yyyy-MM-dd

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)

        txtMonthYear = view.findViewById(R.id.txtMonthYear)
        calendarRecyclerView = view.findViewById(R.id.calendarRecyclerView)
        txtSelectedDay = view.findViewById(R.id.txtSelectedDay)
        btnSetPeriodStart = view.findViewById(R.id.btnSetPeriodStart)
        btnSaveSymptoms = view.findViewById(R.id.btnSaveSymptoms)

        // Symptoms
        chipCramps = view.findViewById(R.id.chipCramps)
        chipHeadache = view.findViewById(R.id.chipHeadache)
        chipFatigue = view.findViewById(R.id.chipFatigue)
        chipMood = view.findViewById(R.id.chipMood)
        chipBloating = view.findViewById(R.id.chipBloating)

        val symptomsChips = mapOf(
            "Cramps" to chipCramps,
            "Headache" to chipHeadache,
            "Fatigue" to chipFatigue,
            "Mood Swings" to chipMood,
            "Bloating" to chipBloating
        )

        // Setup Symptoms Toggle
        symptomsChips.forEach { (name, chip) ->
            chip.setOnClickListener {
                if (selectedSymptoms.contains(name)) {
                    selectedSymptoms.remove(name)
                    chip.setBackgroundResource(R.drawable.bg_task_pending)
                    chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendarBrown))
                } else {
                    selectedSymptoms.add(name)
                    chip.setBackgroundResource(R.drawable.bg_task_done)
                    chip.setTextColor(Color.WHITE)
                }
            }
        }

        // Init Calendar State
        calendar = Calendar.getInstance()
        selectedDate = Calendar.getInstance()

        view.findViewById<TextView>(R.id.btnPrevMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            refreshCalendar()
        }

        view.findViewById<TextView>(R.id.btnNextMonth).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            refreshCalendar()
        }

        btnSetPeriodStart.setOnClickListener {
            setPeriodStart()
        }

        btnSaveSymptoms.setOnClickListener {
            saveSymptoms()
        }

        calendarRecyclerView.layoutManager = GridLayoutManager(context, 7)

        fetchUserData()

        return view
    }

    private fun fetchUserData() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded) {
                    val cycle = doc.get("cycle") as? Map<*, *>
                    cycleLength = (cycle?.get("cycleLength") as? Long)?.toInt() ?: 28
                    lastPeriodDate = cycle?.get("lastPeriodDate") as? String ?: ""

                    // Fetch logged symptoms subcollection or field
                    val symptomLogs = doc.get("symptomLogs") as? Map<*, *>
                    symptomLogs?.forEach { (k, v) ->
                        val dateKey = k as? String ?: ""
                        val list = v as? List<*>
                        if (dateKey.isNotEmpty() && list != null) {
                            loggedSymptomsMap[dateKey] = list.filterIsInstance<String>()
                        }
                    }

                    refreshCalendar()
                    updateSelectedDateUI()
                }
            }
    }

    private fun refreshCalendar() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        txtMonthYear.text = sdf.format(calendar.time)

        val daysList = ArrayList<CalendarDay>()
        val monthCalendar = calendar.clone() as Calendar
        monthCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK)
        // Adjust standard calendar where Sun is 1st day to Mon being 1st day
        var offset = firstDayOfWeek - 2
        if (offset < 0) offset += 7

        monthCalendar.add(Calendar.DAY_OF_MONTH, -offset)

        // 42 days grid layout
        while (daysList.size < 42) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(monthCalendar.time)
            val isCurrentMonth = monthCalendar.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
            val isSelected = isSameDay(monthCalendar, selectedDate)

            // Compute cycle day and status based on lastPeriodDate
            var isPeriod = false
            var isFertility = false

            if (lastPeriodDate.isNotEmpty()) {
                val sdfParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                try {
                    val last = sdfParser.parse(lastPeriodDate)
                    if (last != null) {
                        val diffMs = monthCalendar.timeInMillis - last.time
                        val daysDiff = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                        if (daysDiff >= 0) {
                            val cycleDay = (daysDiff % cycleLength) + 1
                            if (cycleDay in 1..5) {
                                isPeriod = true
                            } else if (cycleDay in 11..17) {
                                isFertility = true
                            }
                        }
                    }
                } catch (e: Exception) {}
            }

            daysList.add(
                CalendarDay(
                    dayNumber = monthCalendar.get(Calendar.DAY_OF_MONTH).toString(),
                    dateString = dateStr,
                    isCurrentMonth = isCurrentMonth,
                    isSelected = isSelected,
                    isPeriod = isPeriod,
                    isFertility = isFertility,
                    calendarRef = monthCalendar.clone() as Calendar
                )
            )
            monthCalendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        calendarRecyclerView.adapter = CalendarAdapter(daysList) { day ->
            selectedDate = day.calendarRef
            updateSelectedDateUI()
            refreshCalendar()
        }
    }

    private fun updateSelectedDateUI() {
        val sdf = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        val dateStr = sdf.format(selectedDate.time)
        txtSelectedDay.text = "Logs for $dateStr"

        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
        selectedSymptoms.clear()
        val saved = loggedSymptomsMap[dateKey] ?: emptyList()
        selectedSymptoms.addAll(saved)

        // Update chips UI
        val symptomsChips = mapOf(
            "Cramps" to chipCramps,
            "Headache" to chipHeadache,
            "Fatigue" to chipFatigue,
            "Mood Swings" to chipMood,
            "Bloating" to chipBloating
        )

        symptomsChips.forEach { (name, chip) ->
            if (selectedSymptoms.contains(name)) {
                chip.setBackgroundResource(R.drawable.bg_task_done)
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.bg_task_pending)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendarBrown))
            }
        }
    }

    private fun setPeriodStart() {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).update("cycle.lastPeriodDate", dateKey)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Period start logged successfully!", Toast.LENGTH_SHORT).show()
                    lastPeriodDate = dateKey
                    refreshCalendar()
                }
            }
    }

    private fun saveSymptoms() {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
        val currentUser = auth.currentUser ?: return

        loggedSymptomsMap[dateKey] = selectedSymptoms.toList()

        db.collection("users").document(currentUser.uid).update("symptomLogs.$dateKey", selectedSymptoms.toList())
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Symptoms logged successfully!", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // Data Class & Adapter
    data class CalendarDay(
        val dayNumber: String,
        val dateString: String,
        val isCurrentMonth: Boolean,
        val isSelected: Boolean,
        val isPeriod: Boolean,
        val isFertility: Boolean,
        val calendarRef: Calendar
    )

    inner class CalendarAdapter(
        private val list: List<CalendarDay>,
        private val onClick: (CalendarDay) -> Unit
    ) : RecyclerView.Adapter<CalendarAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val dateText: TextView = v.findViewById(R.id.dateText)
            val periodDot: View = v.findViewById(R.id.periodDot)
            val fertilityDot: View = v.findViewById(R.id.fertilityDot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val day = list[position]
            holder.dateText.text = day.dayNumber

            // Colors/State
            if (!day.isCurrentMonth) {
                holder.dateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.snehMuted))
            } else {
                holder.dateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.calendarBrown))
            }

            if (day.isSelected) {
                holder.dateText.setBackgroundResource(R.drawable.bg_selected_date)
                holder.dateText.setTextColor(Color.WHITE)
            } else if (day.isPeriod) {
                holder.dateText.setBackgroundResource(R.drawable.bg_period_day)
                holder.dateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.snehRose))
            } else if (day.isFertility) {
                holder.dateText.setBackgroundResource(R.drawable.bg_ovulation_day)
                holder.dateText.setTextColor(ContextCompat.getColor(requireContext(), R.color.snehDeep))
            } else {
                holder.dateText.background = null
            }

            // Dots
            holder.periodDot.visibility = if (day.isPeriod) View.VISIBLE else View.GONE
            holder.fertilityDot.visibility = if (day.isFertility) View.VISIBLE else View.GONE

            holder.itemView.setOnClickListener {
                onClick(day)
            }
        }

        override fun getItemCount(): Int = list.size
    }
}
