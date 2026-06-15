package com.sneh.app.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sneh.app.R
import com.sneh.app.core.CycleUtils

class AiChatFragment : Fragment() {

    private lateinit var chatScrollView: ScrollView
    private lateinit var chatMessagesContainer: LinearLayout
    private lateinit var editChatMessage: EditText
    private lateinit var btnSendChat: ImageView

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var cycleLength = 28
    private var lastPeriodDate = ""
    private var userName = "Priya"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_ai_chat, container, false)

        chatScrollView = view.findViewById(R.id.chatScrollView)
        chatMessagesContainer = view.findViewById(R.id.chatMessagesContainer)
        editChatMessage = view.findViewById(R.id.editChatMessage)
        btnSendChat = view.findViewById(R.id.btnSendChat)

        btnSendChat.setOnClickListener {
            sendMessage()
        }

        fetchUserData()

        return view
    }

    private fun fetchUserData() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (isAdded) {
                    val profile = doc.get("profile") as? Map<*, *>
                    userName = profile?.get("name") as? String ?: "Priya"

                    val cycle = doc.get("cycle") as? Map<*, *>
                    cycleLength = (cycle?.get("cycleLength") as? Long)?.toInt() ?: 28
                    lastPeriodDate = cycle?.get("lastPeriodDate") as? String ?: ""

                    fetchChatHistory(currentUser.uid)
                }
            }
            .addOnFailureListener {
                addWelcomeMessage()
            }
    }

    private fun fetchChatHistory(userId: String) {
        db.collection("users").document(userId)
            .collection("chats")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { docs ->
                if (isAdded) {
                    chatMessagesContainer.removeAllViews()
                    if (docs.isEmpty) {
                        addWelcomeMessage()
                    } else {
                        for (doc in docs) {
                            val text = doc.getString("text") ?: ""
                            val isAi = doc.getBoolean("isAi") ?: false
                            addMessageBubble(text, isAi)
                        }
                    }
                }
            }
            .addOnFailureListener {
                addWelcomeMessage()
            }
    }

    private fun addWelcomeMessage() {
        if (!isAdded) return
        chatMessagesContainer.removeAllViews()

        val welcomeText = "Hello $userName! How are you feeling today? I am your Sneh AI assistant, here to provide insights based on your cycle logs."
        addMessageBubble(welcomeText, isAi = true)
    }

    private fun sendMessage() {
        val messageText = editChatMessage.text.toString().trim()
        if (messageText.isEmpty()) return

        editChatMessage.text.clear()
        addMessageBubble(messageText, isAi = false)

        val currentUser = auth.currentUser ?: return
        val userMsgPayload = mapOf(
            "text" to messageText,
            "isAi" to false,
            "timestamp" to System.currentTimeMillis()
        )
        db.collection("users").document(currentUser.uid)
            .collection("chats").add(userMsgPayload)

        // Generate AI response
        btnSendChat.postDelayed({
            if (isAdded) {
                val aiResponse = generateAiResponse(messageText)
                addMessageBubble(aiResponse, isAi = true)

                val aiMsgPayload = mapOf(
                    "text" to aiResponse,
                    "isAi" to true,
                    "timestamp" to System.currentTimeMillis()
                )
                db.collection("users").document(currentUser.uid)
                    .collection("chats").add(aiMsgPayload)
            }
        }, 1000)
    }

    private fun generateAiResponse(query: String): String {
        val lower = query.lowercase()
        val (day, phase) = if (lastPeriodDate.isNotEmpty()) {
            CycleUtils.getCycleDayAndPhase(lastPeriodDate, cycleLength)
        } else {
            1 to "Unknown"
        }

        return when {
            lower.contains("tired") || lower.contains("fatigue") || lower.contains("sleepy") -> {
                "Based on your logs, you're currently in the $phase (Day $day). Fatigue can be common here due to hormone fluctuations. Make sure you get at least 7-8 hours of sleep and supplement with iron-rich foods like spinach."
            }
            lower.contains("cramp") || lower.contains("pain") || lower.contains("hurt") -> {
                "For cramps, gentle yoga (like Child's pose and Supine Twists) is excellent. A warm bath or ginger tea can also help ease lower abdominal discomfort. Avoid high intensity cardio workouts today."
            }
            lower.contains("ovulation") || lower.contains("fertile") || lower.contains("conceive") -> {
                "Based on your 28-day cycle, ovulation usually peaks around Day 14. Your fertile window is active between Day 11 and Day 17. Conception chances are highest during these days."
            }
            lower.contains("diet") || lower.contains("food") || lower.contains("eat") -> {
                "To optimize nutrition in the $phase, focus on balanced meals. If you are menstruating, prioritize iron and magnesium. During the follicular phase, eat lean protein and complex carbs."
            }
            else -> {
                "I've analyzed your wellness logs. For the $phase (Day $day), maintaining steady hydration, a light 20-minute walk, and a balanced diet will help support your energy and flow."
            }
        }
    }

    private fun addMessageBubble(text: String, isAi: Boolean) {
        val context = context ?: return
        val bubble = TextView(context).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 6, 0, 6)
                gravity = if (isAi) Gravity.START else Gravity.END
            }
            layoutParams = lp
            this.text = text
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
            setPadding(14, 10, 14, 10)

            if (isAi) {
                setBackgroundResource(R.drawable.bg_chat_ai)
                setTextColor(ContextCompat.getColor(context, R.color.snehText))
            } else {
                setBackgroundResource(R.drawable.bg_chat_user)
                setTextColor(Color.WHITE)
            }
        }

        chatMessagesContainer.addView(bubble)
        chatScrollView.post {
            chatScrollView.fullScroll(View.FOCUS_DOWN)
        }
    }
}
