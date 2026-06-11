package com.sneh.app.ui.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.sneh.app.R
import java.text.SimpleDateFormat
import java.util.*

class ExpertCommunityFragment : Fragment() {

    private lateinit var editCommunityQuestion: EditText
    private lateinit var btnPostQuestion: Button
    private lateinit var communityFeedContainer: LinearLayout

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

        btnPostQuestion.setOnClickListener {
            postQuestion()
        }

        fetchCommunityQuestions()

        return view
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
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("communityQuestions").add(payload)
            .addOnSuccessListener {
                if (isAdded) {
                    editCommunityQuestion.text.clear()
                    Toast.makeText(context, "Question posted successfully!", Toast.LENGTH_SHORT).show()
                    fetchCommunityQuestions() // Refresh feed
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
                            val author = doc.getString("author") ?: "Anonymous"
                            val text = doc.getString("text") ?: ""
                            val ts = doc.getLong("timestamp") ?: 0L
                            addQuestionToFeedUI(author, text, ts)
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
        // Mock default questions for aesthetic completeness
        val defaults = listOf(
            Triple("Aarushi", "Is it normal to crave chocolate and sweets during my luteal phase?", System.currentTimeMillis() - 3600000),
            Triple("Ananya", "What are the best yoga exercises to relieve painful cramps during day 2 of period?", System.currentTimeMillis() - 7200000),
            Triple("Kirti", "How does PCOS affect the length of my cycle, and how does Sneh adjust predictions for it?", System.currentTimeMillis() - 14400000)
        )

        defaults.forEach { (author, text, ts) ->
            addQuestionToFeedUI(author, text, ts)
        }
    }

    private fun addQuestionToFeedUI(author: String, questionContent: String, timestamp: Long) {
        val context = context ?: return
        val itemContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            layoutParams = lp
            setBackgroundResource(R.drawable.bg_task_pending)
            setPadding(12, 12, 12, 12)
        }

        val authorText = TextView(context).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
            text = "Posted by $author"
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 9f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_bold)
            setTextColor(ContextCompat.getColor(context, R.color.snehRose))
        }

        val questionText = TextView(context).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            layoutParams = lp
            text = questionContent
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
            setTextColor(ContextCompat.getColor(context, R.color.snehText))
        }

        val timeText = TextView(context).apply {
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams = lp
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            text = sdf.format(Date(timestamp))
            setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 8f)
            typeface = ResourcesCompat.getFont(context, R.font.poppins_regular)
            setTextColor(ContextCompat.getColor(context, R.color.snehMuted))
        }

        itemContainer.addView(authorText)
        itemContainer.addView(questionText)
        itemContainer.addView(timeText)

        communityFeedContainer.addView(itemContainer)
    }
}
