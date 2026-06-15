package com.sneh.app.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sneh.app.R
import java.text.SimpleDateFormat
import java.util.*

class ExpertCommunityFragment : Fragment() {

    private lateinit var editCommunityQuestion: EditText
    private lateinit var btnPostQuestion: Button
    private lateinit var communityFeedContainer: LinearLayout
    private lateinit var llExpertsContainer: LinearLayout

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_expert_community, container, false)

        editCommunityQuestion = view.findViewById(R.id.editCommunityQuestion)
        btnPostQuestion = view.findViewById(R.id.btnPostQuestion)
        communityFeedContainer = view.findViewById(R.id.communityFeedContainer)
        llExpertsContainer = view.findViewById(R.id.llExpertsContainer)

        btnPostQuestion.setOnClickListener {
            postQuestion()
        }

        // Fetch Experts & Community questions dynamically
        fetchExperts()
        fetchCommunityQuestions()

        return view
    }

    private fun fetchExperts() {
        db.collection("experts")
            .get()
            .addOnSuccessListener { docs ->
                if (!isAdded) return@addOnSuccessListener
                llExpertsContainer.removeAllViews()
                if (docs.isEmpty) {
                    seedDefaultExperts()
                } else {
                    for (doc in docs) {
                        val name = doc.getString("name") ?: "Expert"
                        val title = doc.getString("title") ?: "Hormonal Coach"
                        val emoji = doc.getString("emoji") ?: "👩‍⚕️"
                        val isOnline = doc.getBoolean("isOnline") ?: true
                        addExpertToUI(name, title, emoji, isOnline)
                    }
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    seedDefaultExperts()
                }
            }
    }

    private fun seedDefaultExperts() {
        val defaults = listOf(
            mapOf("name" to "Dr. Ananya Sharma", "title" to "Gynecologist • 12 yrs exp", "emoji" to "👩‍⚕️", "isOnline" to true),
            mapOf("name" to "Meera Nair", "title" to "Yoga & Hormonal Coach • 8 yrs exp", "emoji" to "🧘‍♀️", "isOnline" to true),
            mapOf("name" to "Dr. Sarah Collins", "title" to "Fertility Specialist • 15 yrs exp", "emoji" to "👩‍⚕️", "isOnline" to false)
        )

        llExpertsContainer.removeAllViews()
        for (exp in defaults) {
            val name = exp["name"] as String
            val title = exp["title"] as String
            val emoji = exp["emoji"] as String
            val isOnline = exp["isOnline"] as Boolean

            addExpertToUI(name, title, emoji, isOnline)
            db.collection("experts").add(exp)
        }
    }

    private fun addExpertToUI(name: String, title: String, emoji: String, isOnline: Boolean) {
        val context = context ?: return

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        val avatar = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                (36 * resources.displayMetrics.density).toInt(),
                (36 * resources.displayMetrics.density).toInt()
            ).apply {
                marginEnd = (12 * resources.displayMetrics.density).toInt()
            }
            text = emoji
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = android.view.Gravity.CENTER
        }

        val textLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val nameTxt = TextView(context).apply {
            text = name
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            setTextColor(ContextCompat.getColor(context, R.color.snehText))
        }

        val titleTxt = TextView(context).apply {
            text = title
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
            setTextColor(ContextCompat.getColor(context, R.color.snehSubtle))
        }

        textLayout.addView(nameTxt)
        textLayout.addView(titleTxt)

        val statusTxt = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = if (isOnline) "● Online" else "○ Offline"
            setTextColor(if (isOnline) Color.parseColor("#1E6B52") else Color.parseColor("#7A5545"))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            setPadding(16, 4, 16, 4)
        }

        val consultBtn = com.google.android.material.card.MaterialCardView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = (8 * resources.displayMetrics.density).toInt()
            }
            cardElevation = 0f
            radius = 12 * resources.displayMetrics.density
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.snehRose))
            isClickable = true
            isFocusable = true

            val btnTxt = TextView(context).apply {
                text = "Consult"
                setTextColor(Color.WHITE)
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9f)
                typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
                setPadding(20, 10, 20, 10)
            }
            addView(btnTxt)

            setOnClickListener {
                Toast.makeText(context, "Consultation requested with $name!", Toast.LENGTH_SHORT).show()
            }
        }

        row.addView(avatar)
        row.addView(textLayout)
        row.addView(statusTxt)
        row.addView(consultBtn)

        llExpertsContainer.addView(row)
    }

    private fun postQuestion() {
        val questionText = editCommunityQuestion.text.toString().trim()
        if (questionText.isEmpty()) {
            Toast.makeText(context, "Please type a question", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser ?: return
        val userEmail = currentUser.email ?: "Anonymous"
        val displayName = userEmail.substringBefore("@")

        val payload = mapOf(
            "author" to displayName,
            "text" to questionText,
            "timestamp" to System.currentTimeMillis(),
            "likes" to 0,
            "answers" to emptyList<Map<String, Any>>()
        )

        db.collection("communityQuestions").add(payload)
            .addOnSuccessListener {
                if (isAdded) {
                    editCommunityQuestion.text.clear()
                    Toast.makeText(context, "Question posted successfully!", Toast.LENGTH_SHORT).show()
                    fetchCommunityQuestions()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to post question", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchCommunityQuestions() {
        db.collection("communityQuestions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { docs ->
                if (isAdded) {
                    communityFeedContainer.removeAllViews()
                    if (docs.isEmpty) {
                        seedDefaultQuestions()
                    } else {
                        for (doc in docs) {
                            val id = doc.id
                            val author = doc.getString("author") ?: "Anonymous"
                            val text = doc.getString("text") ?: ""
                            val ts = doc.getLong("timestamp") ?: 0L
                            val likes = doc.getLong("likes")?.toInt() ?: 0
                            val answers = doc.get("answers") as? List<*> ?: emptyList<Any>()
                            
                            val answersList = mutableListOf<Map<String, Any>>()
                            for (item in answers) {
                                if (item is Map<*, *>) {
                                    @Suppress("UNCHECKED_CAST")
                                    answersList.add(item as Map<String, Any>)
                                }
                            }
                            addQuestionToFeedUI(id, author, text, ts, likes, answersList)
                        }
                    }
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    communityFeedContainer.removeAllViews()
                    seedDefaultQuestions()
                }
            }
    }

    private fun seedDefaultQuestions() {
        val defaults = listOf(
            mapOf(
                "author" to "Aarushi",
                "text" to "Is it normal to crave chocolate and sweets during my luteal phase?",
                "timestamp" to System.currentTimeMillis() - 3600000,
                "likes" to 5,
                "answers" to listOf(
                    mapOf("author" to "Meera Nair", "text" to "Absolutely! Progesterone peaks in the luteal phase, causing blood sugar levels to fluctuate. Focus on eating dark chocolate or complex carbs.", "timestamp" to System.currentTimeMillis() - 1800000)
                )
            ),
            mapOf(
                "author" to "Ananya",
                "text" to "What are the best yoga exercises to relieve painful cramps during day 2 of period?",
                "timestamp" to System.currentTimeMillis() - 7200000,
                "likes" to 3,
                "answers" to emptyList<Map<String, Any>>()
            ),
            mapOf(
                "author" to "Kirti",
                "text" to "How does PCOS affect the length of my cycle, and how does Sneh adjust predictions for it?",
                "timestamp" to System.currentTimeMillis() - 14400000,
                "likes" to 8,
                "answers" to emptyList<Map<String, Any>>()
            )
        )

        communityFeedContainer.removeAllViews()
        for (q in defaults) {
            db.collection("communityQuestions").add(q)
                .addOnSuccessListener { docRef ->
                    val id = docRef.id
                    val author = q["author"] as String
                    val text = q["text"] as String
                    val ts = q["timestamp"] as Long
                    val likes = q["likes"] as Int
                    @Suppress("UNCHECKED_CAST")
                    val answers = q["answers"] as List<Map<String, Any>>
                    if (isAdded) {
                        addQuestionToFeedUI(id, author, text, ts, likes, answers)
                    }
                }
        }
    }

    private fun addQuestionToFeedUI(
        questionId: String,
        author: String,
        questionContent: String,
        timestamp: Long,
        likes: Int,
        answers: List<Map<String, Any>>
    ) {
        val context = context ?: return
        
        val itemContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 12, 0, 12)
            }
            layoutParams = lp
            setBackgroundResource(R.drawable.bg_task_pending)
            setPadding(24, 20, 24, 20)
        }

        val headerLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val authorText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = "Posted by $author"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            setTextColor(ContextCompat.getColor(context, R.color.snehRose))
        }

        val timeText = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            text = sdf.format(Date(timestamp))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 8f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
            setTextColor(ContextCompat.getColor(context, R.color.snehMuted))
        }

        headerLayout.addView(authorText)
        headerLayout.addView(timeText)

        val questionText = TextView(context).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            layoutParams = lp
            text = questionContent
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
            setTextColor(ContextCompat.getColor(context, R.color.snehText))
        }

        val actionRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 8)
            }
            layoutParams = lp
        }

        val likeBtn = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "❤️ $likes Likes"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            setTextColor(ContextCompat.getColor(context, R.color.snehRose))
            setPadding(0, 8, 16, 8)
            isClickable = true
            isFocusable = true
            
            setOnClickListener {
                incrementLike(questionId)
            }
        }

        val replyLabel = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "💬 ${answers.size} Replies"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            setTextColor(ContextCompat.getColor(context, R.color.calendarBrown))
            setPadding(16, 8, 16, 8)
        }

        actionRow.addView(likeBtn)
        actionRow.addView(replyLabel)

        val divider = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(0, 4, 0, 8)
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.snehBlush))
        }

        val repliesContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        for (ans in answers) {
            val replyAuthor = ans["author"] as? String ?: "Expert"
            val replyText = ans["text"] as? String ?: ""
            val replyTs = ans["timestamp"] as? Long ?: System.currentTimeMillis()

            val replyRow = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 4, 0, 4)
                }
                setBackgroundColor(Color.parseColor("#08000000"))
                setPadding(12, 8, 12, 8)
            }

            val replyHeader = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            
            val replyAuthorTxt = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = "Reply from $replyAuthor"
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9f)
                typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
                setTextColor(ContextCompat.getColor(context, R.color.snehDeep))
            }

            val replyTimeTxt = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                text = sdf.format(Date(replyTs))
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 8f)
                typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
                setTextColor(ContextCompat.getColor(context, R.color.snehMuted))
            }

            replyHeader.addView(replyAuthorTxt)
            replyHeader.addView(replyTimeTxt)

            val replyContentTxt = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 2, 0, 0)
                }
                text = replyText
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
                typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
                setTextColor(ContextCompat.getColor(context, R.color.snehText))
            }

            replyRow.addView(replyHeader)
            replyRow.addView(replyContentTxt)
            repliesContainer.addView(replyRow)
        }

        val replyInputLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 0)
            }
        }

        val etReply = EditText(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                setMargins(0, 0, 8, 0)
            }
            hint = "Write a reply..."
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
            setTextColor(ContextCompat.getColor(context, R.color.snehText))
            setPadding(16, 12, 16, 12)
            setBackgroundResource(R.drawable.bg_input)
        }

        val btnReply = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = "Reply"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            setTextColor(ContextCompat.getColor(context, R.color.snehRose))
            setPadding(16, 12, 16, 12)
            isClickable = true
            isFocusable = true
            
            setOnClickListener {
                val replyText = etReply.text.toString().trim()
                if (replyText.isNotEmpty()) {
                    postReply(questionId, replyText)
                }
            }
        }

        replyInputLayout.addView(etReply)
        replyInputLayout.addView(btnReply)

        itemContainer.addView(headerLayout)
        itemContainer.addView(questionText)
        itemContainer.addView(actionRow)
        itemContainer.addView(divider)
        itemContainer.addView(repliesContainer)
        itemContainer.addView(replyInputLayout)

        communityFeedContainer.addView(itemContainer)
    }

    private fun incrementLike(questionId: String) {
        db.collection("communityQuestions").document(questionId)
            .get()
            .addOnSuccessListener { doc ->
                val currentLikes = doc.getLong("likes") ?: 0L
                db.collection("communityQuestions").document(questionId)
                    .update("likes", currentLikes + 1)
                    .addOnSuccessListener {
                        fetchCommunityQuestions()
                    }
            }
    }

    private fun postReply(questionId: String, replyText: String) {
        val currentUser = auth.currentUser ?: return
        val userEmail = currentUser.email ?: "Anonymous"
        val displayName = userEmail.substringBefore("@")

        val replyPayload = mapOf(
            "author" to displayName,
            "text" to replyText,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("communityQuestions").document(questionId)
            .update("answers", com.google.firebase.firestore.FieldValue.arrayUnion(replyPayload))
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Reply posted successfully!", Toast.LENGTH_SHORT).show()
                    fetchCommunityQuestions()
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to post reply", Toast.LENGTH_SHORT).show()
            }
    }
}
