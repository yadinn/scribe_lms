package com.example.scribelms.student

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.helpdesk.ChatbotEngine
import com.example.scribelms.helpdesk.HelpdeskAdapter
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class StudentHelpdeskActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val botMessages  = mutableListOf<Map<String, Any>>()
    private val liveMessages = mutableListOf<Map<String, Any>>()

    private lateinit var rvMessages:      RecyclerView
    private lateinit var etMessage:       EditText
    private lateinit var btnSend:         MaterialButton
    private lateinit var tvModeLabel:     TextView
    private lateinit var btnToggle:       MaterialButton
    private lateinit var tvTitle:         TextView
    private lateinit var tvTyping:        TextView
    private lateinit var llSearchHint:    LinearLayout
    private lateinit var tvDismissHint:   TextView

    private var botAdapter:  HelpdeskAdapter? = null
    private var liveAdapter: HelpdeskAdapter? = null

    private var liveListener:   ListenerRegistration? = null
    private var typingListener: ListenerRegistration? = null
    private var studentName = ""
    private var threadId    = ""
    private var uid         = ""
    private var isLiveMode  = false
    private var botExchanges = 0   // how many back-and-forth the user has had with bot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_helpdesk)

        uid      = auth.currentUser?.uid ?: run { finish(); return }
        threadId = "student_$uid"

        rvMessages    = findViewById(R.id.rvMessages)
        etMessage     = findViewById(R.id.etMessage)
        btnSend       = findViewById(R.id.btnSend)
        tvModeLabel   = findViewById(R.id.tvModeLabel)
        btnToggle     = findViewById(R.id.btnToggleMode)
        tvTitle       = findViewById(R.id.tvTitle)
        tvTyping      = findViewById(R.id.tvTypingIndicator)
        llSearchHint  = findViewById(R.id.llSearchHint)
        tvDismissHint = findViewById(R.id.tvDismissHint)

        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                studentName = doc.getString("name") ?: "Student"
                tvTitle.text = "🤖 Hi, ${studentName.substringBefore(" ")}!"
            }

        btnSend.setOnClickListener { onSendClicked() }
        btnToggle.setOnClickListener {
            if (isLiveMode) switchToBotMode() else escalateToLiveStaff()
        }

        // Search hint dismiss
        tvDismissHint.setOnClickListener {
            llSearchHint.visibility = View.GONE
        }

        switchToBotMode()
        showBotWelcome()
    }

    // ── BOT MODE ──────────────────────────────────────────────────────────────

    private fun switchToBotMode() {
        isLiveMode = false
        liveListener?.remove(); liveListener = null
        typingListener?.remove(); typingListener = null

        tvTitle.text     = "🤖 Library Assistant"
        tvModeLabel.text = "Ask me anything about the library"
        etMessage.hint   = "Type your question..."
        btnToggle.visibility = View.GONE

        botAdapter = HelpdeskAdapter(botMessages, uid) { reply -> handleBotInput(reply) }
        rvMessages.adapter = botAdapter
        scrollToBottom(botMessages.size)
        // Show search hint after a moment
        Handler(Looper.getMainLooper()).postDelayed({
            llSearchHint.visibility = View.VISIBLE
        }, 1500)
    }

    private fun showBotWelcome() {
        addBotMsg(
            "👋 Hello! I'm your Library Assistant.\n\n" +
            "I can help you with:\n" +
            "• Borrowing & returning books\n" +
            "• Finding books in the library\n" +
            "• Fines, due dates, e-books\n\n" +
            "What would you like to know?",
            listOf("📚 How to borrow?", "🔍 Find a book", "💰 Check fines",
                   "📦 How to return?", "📘 E-Books", "📞 Contact library")
        )
    }

    private fun handleBotInput(input: String) {
        // Add user message
        botMessages.add(buildUserMsg(input))
        botAdapter?.notifyItemInserted(botMessages.size - 1)
        scrollToBottom(botMessages.size)
        botExchanges++

        // After 2 exchanges, show the "Talk to Staff" button
        if (botExchanges >= 2) {
            btnToggle.text = "👤 Talk to Staff"
            btnToggle.visibility = View.VISIBLE
        }

        // Show typing indicator
        tvTyping.text = "🤖 Library Bot is typing..."
        tvTyping.visibility = View.VISIBLE

        // Simulate slight delay for natural feel
        Handler(Looper.getMainLooper()).postDelayed({
            ChatbotEngine.processInput(input) { response ->
                tvTyping.visibility = View.GONE

                addBotMsg(response.text, response.quickReplies)

                // Auto escalate if user explicitly asked
                if (response.type == ChatbotEngine.ResponseType.ESCALATE) {
                    btnToggle.text = "👤 Connect to Staff"
                    btnToggle.visibility = View.VISIBLE
                }
            }
        }, 600)
    }

    private fun addBotMsg(text: String, quickReplies: List<String>) {
        val prevLast = botMessages.size - 1
        botMessages.add(buildBotMsg(text, quickReplies))
        if (prevLast >= 0) botAdapter?.notifyItemChanged(prevLast)
        botAdapter?.notifyItemInserted(botMessages.size - 1)
        scrollToBottom(botMessages.size)
        // Animate in
        rvMessages.post {
            val lastView = (rvMessages.layoutManager as LinearLayoutManager)
                .findViewByPosition(botMessages.size - 1)
            lastView?.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade))
        }
    }

    private fun buildBotMsg(text: String, quickReplies: List<String>): Map<String, Any> {
        val m = mutableMapOf<String, Any>(
            "senderUid"  to "bot",
            "senderName" to "Library Bot",
            "senderRole" to "bot",
            "text"       to text,
            "timestamp"  to Timestamp.now()
        )
        if (quickReplies.isNotEmpty()) m["quickReplies"] = quickReplies
        return m
    }

    private fun buildUserMsg(text: String): Map<String, Any> = mapOf(
        "senderUid"  to uid,
        "senderName" to studentName,
        "senderRole" to "student",
        "text"       to text,
        "timestamp"  to Timestamp.now()
    )

    // ── LIVE STAFF MODE ───────────────────────────────────────────────────────

    private fun escalateToLiveStaff() {
        isLiveMode = true
        llSearchHint.visibility = View.GONE
        tvTitle.text     = "👤 Live Staff Chat"
        tvModeLabel.text = "Staff will reply during library hours"
        btnToggle.text   = "🤖 Back to Bot"
        etMessage.hint   = "Message to library staff..."

        db.collection("helpdesk_threads").document(threadId)
            .set(mapOf(
                "studentUid"    to uid,
                "studentName"   to studentName,
                "updatedAt"     to Timestamp.now(),
                "unreadStaff"   to true,
                "unreadStudent" to false
            ), SetOptions.merge())

        db.collection("helpdesk_threads").document(threadId)
            .collection("messages").add(mapOf(
                "senderUid"  to "system",
                "senderName" to "System",
                "senderRole" to "system",
                "text"       to "📢 $studentName has requested live staff assistance.",
                "timestamp"  to Timestamp.now()
            ))
        db.collection("helpdesk_threads").document(threadId)
            .update(mapOf("lastMessage" to "Live chat started",
                          "updatedAt" to Timestamp.now(), "unreadStaff" to true))

        liveAdapter = HelpdeskAdapter(liveMessages, uid)
        rvMessages.adapter = liveAdapter

        // Listen for typing from staff
        listenForStaffTyping()

        liveListener = db.collection("helpdesk_threads").document(threadId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                liveMessages.clear()
                for (doc in snap.documents) liveMessages.add(doc.data ?: continue)
                liveAdapter?.notifyDataSetChanged()
                if (liveMessages.isNotEmpty()) scrollToBottom(liveMessages.size)
                db.collection("helpdesk_threads").document(threadId)
                    .update("unreadStudent", false)
            }
    }

    private fun listenForStaffTyping() {
        typingListener = db.collection("helpdesk_threads").document(threadId)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                val staffTyping = snap.getBoolean("staffTyping") ?: false
                if (staffTyping && isLiveMode) {
                    tvTyping.text = "👤 Staff is typing..."
                    tvTyping.visibility = View.VISIBLE
                } else {
                    tvTyping.visibility = View.GONE
                }
            }
    }

    private fun sendLiveMessage(text: String) {
        // Show student typing to staff
        db.collection("helpdesk_threads").document(threadId)
            .update("studentTyping", true)

        val msg = hashMapOf(
            "senderUid"  to uid,
            "senderName" to studentName,
            "senderRole" to "student",
            "text"       to text,
            "timestamp"  to Timestamp.now(),
            "threadId"   to threadId
        )
        db.collection("helpdesk_threads").document(threadId)
            .collection("messages").add(msg)
        db.collection("helpdesk_threads").document(threadId)
            .update(mapOf("lastMessage" to text, "updatedAt" to Timestamp.now(),
                          "unreadStaff" to true, "studentTyping" to false))
    }

    private fun onSendClicked() {
        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return
        etMessage.setText("")
        if (isLiveMode) sendLiveMessage(text) else handleBotInput(text)
    }

    private fun scrollToBottom(size: Int) {
        rvMessages.post { if (size > 0) rvMessages.smoothScrollToPosition(size - 1) }
    }

    override fun onDestroy() {
        super.onDestroy()
        liveListener?.remove()
        typingListener?.remove()
    }
}
