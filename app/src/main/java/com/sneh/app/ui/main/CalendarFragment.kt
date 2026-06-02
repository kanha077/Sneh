package com.sneh.app.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
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

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var recyclerView: RecyclerView
    private lateinit var monthYearText: TextView
    private var currentCalendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_calendar, container, false)
        
        recyclerView = view.findViewById(R.id.calendarRecyclerView)
        monthYearText = view.findViewById(R.id.monthYearText)
        val backBtn = view.findViewById<ImageButton>(R.id.backBtn)
        val nextMonthBtn = view.findViewById<ImageButton>(R.id.nextMonthBtn)
        val fabLog = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabLog)
        val mainCTA = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.mainCTA)
        val statusText = view.findViewById<TextView>(R.id.statusText)

        backBtn.setOnClickListener { parentFragmentManager.popBackStack() }
        nextMonthBtn.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateCalendar()
        }

        fabLog.setOnClickListener {
            Toast.makeText(context, "Quick Log: What's on your mind?", Toast.LENGTH_SHORT).show()
        }

        mainCTA.setOnClickListener {
            Toast.makeText(context, "Tracking this phase for you 🌸", Toast.LENGTH_SHORT).show()
        }

        updateCalendar()
        return view
    }

    private fun updateCalendar() {
        val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearText.text = sdfMonth.format(currentCalendar.time)

        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val cycle = doc.get("cycle") as? Map<*, *>
                val cycleLength = (cycle?.get("cycleLength") as? Long)?.toInt() ?: 28
                val lastDate = cycle?.get("lastPeriodDate") as? String ?: ""
                
                // Update Status Text based on current date
                val (day, phase) = getCycleDayAndPhase(lastDate, cycleLength)
                view?.findViewById<TextView>(R.id.statusText)?.text = "Day $day | $phase"

                setupRecyclerView(lastDate, cycleLength)
            }
    }

    private fun getCycleDayAndPhase(lastPeriod: String, cycleLength: Int): Pair<Int, String> {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val last = sdf.parse(lastPeriod) ?: return 1 to "Unknown"
            val today = Date()
            val days = ((today.time - last.time) / (1000 * 60 * 60 * 24)).toInt()
            val cycleDay = (days % cycleLength) + 1
            val phase = when (cycleDay) {
                in 1..5 -> "Period"
                in 14..16 -> "Ovulation"
                else -> "Regular"
            }
            cycleDay to phase
        } catch (e: Exception) { 1 to "Unknown" }
    }

    private fun setupRecyclerView(lastPeriod: String, cycleLength: Int) {
        val daysInMonth = currentCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfMonth = currentCalendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK) - 1
        
        val days = mutableListOf<String>()
        for (i in 0 until firstDayOfMonth) days.add("")
        for (i in 1..daysInMonth) days.add(i.toString())

        recyclerView.layoutManager = GridLayoutManager(context, 7)
        recyclerView.adapter = CalendarAdapter(days, lastPeriod, cycleLength, currentCalendar)
    }

    inner class CalendarAdapter(
        private val days: List<String>,
        private val lastPeriod: String,
        private val cycleLength: Int,
        private val calendar: Calendar
    ) : RecyclerView.Adapter<CalendarAdapter.ViewHolder>() {

        private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val last = try { sdf.parse(lastPeriod) } catch (e: Exception) { null }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val dateText: TextView = view.findViewById(R.id.dateText)
            val periodDot: View = view.findViewById(R.id.periodDot)
            val fertilityDot: View = view.findViewById(R.id.fertilityDot)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_day, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val day = days[position]
            holder.dateText.text = day
            holder.periodDot.visibility = View.GONE
            holder.fertilityDot.visibility = View.GONE
            holder.dateText.setBackgroundColor(Color.TRANSPARENT)

            if (day.isNotEmpty()) {
                val cellDate = calendar.clone() as Calendar
                cellDate.set(Calendar.DAY_OF_MONTH, day.toInt())

                if (last != null) {
                    val diff = ((cellDate.timeInMillis - last.time) / (1000 * 60 * 60 * 24)).toInt()
                    val cycleDay = (diff % cycleLength + cycleLength) % cycleLength + 1
                    
                    if (cycleDay in 1..5) holder.periodDot.visibility = View.VISIBLE
                    if (cycleDay in 14..16) holder.fertilityDot.visibility = View.VISIBLE
                }

                val today = Calendar.getInstance()
                if (cellDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    cellDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                    holder.dateText.setBackgroundResource(R.drawable.bg_selected_date)
                }

                if (cellDate.after(today)) {
                    holder.dateText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.inactivePink))
                } else {
                    holder.dateText.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.calendarBrown))
                }
            }
        }

        override fun getItemCount() = days.size
    }
}