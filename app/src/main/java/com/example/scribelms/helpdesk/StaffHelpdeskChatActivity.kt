package com.example.scribelms.helpdesk

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class StaffHelpdeskChatActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val messages = mutableListOf<Map<String, Any>>()
    private lateinit var adapter:    HelpdeskAdapter
    private lateinit var rv:         RecyclerView
    private lateinit var tvTyping:   TextView
    private var listener:        ListenerRegistration? = null
    private var typingListener:  ListenerRegistration? = null
    private var staffName   = "Staff"
    private var threadId    = ""
    private var studentName = ""
    private var studentUid  = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_helpdesk_chat)

        threadId    = intent.getStringExtra("threadId")    ?: run { finish(); return }
        studentName = intent.getStringExtra("studentName") ?: "Student"
        studentUid  = intent.getStringExtra("studentUid")  ?: ""
        val uid     = auth.currentUser?.uid ?: run { finish(); return }

        rv       = findViewById(R.id.rvMessages)
        tvTyping = findViewById(R.id.tvTypingIndicator)
        rv.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        adapter = HelpdeskAdapter(messages, uid)
        rv.adapter = adapter

        val tvTitle    = findViewById<TextView>(R.id.tvTitle)
        val tvStatus   = findViewById<TextView>(R.id.tvStudentStatus)
        val etMsg      = findViewById<EditText>(R.id.etMessage)
        val btnSend    = findViewById<MaterialButton>(R.id.btnSend)
        val btnHistory = findViewById<MaterialButton>(R.id.btnViewHistory)

        tvTitle.text = "💬 $studentName"

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc -> staffName = doc.getString("name") ?: "Staff" }

        if (studentUid.isNotEmpty()) {
            db.collection("issue_requests")
                .whereEqualTo("studentUid", studentUid)
                .whereEqualTo("status", "issued")
                .get()
                .addOnSuccessListener { snap ->
                    tvStatus.text = "📚 ${snap.size()} book(s) currently issued"
                    tvStatus.visibility = View.VISIBLE
                }
        }

        // Staff typing — write to Firestore so student sees it
        etMsg.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val isTyping = !s.isNullOrEmpty()
                db.collection("helpdesk_threads").document(threadId)
                    .update("staffTyping", isTyping)
            }
        })

        btnSend.setOnClickListener {
            val text = etMsg.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            val msg = hashMapOf(
                "senderUid"  to uid,
                "senderName" to "Staff: $staffName",
                "senderRole" to "staff",
                "text"       to text,
                "timestamp"  to Timestamp.now(),
                "threadId"   to threadId
            )
            db.collection("helpdesk_threads").document(threadId)
                .collection("messages").add(msg)
            db.collection("helpdesk_threads").document(threadId)
                .update(mapOf(
                    "lastMessage"   to text,
                    "updatedAt"     to Timestamp.now(),
                    "unreadStaff"   to false,
                    "unreadStudent" to true,
                    "staffTyping"   to false
                ))
            etMsg.setText("")
        }

        // Quick templates
        val chip1 = findViewById<TextView>(R.id.chipTemplate1)
        val chip2 = findViewById<TextView>(R.id.chipTemplate2)
        val chip3 = findViewById<TextView>(R.id.chipTemplate3)
        chip1.setOnClickListener { etMsg.setText("Your request has been approved! Please show your Collection QR at the counter. ✅") }
        chip2.setOnClickListener { etMsg.setText("Your book is due for return. Please return it to avoid fines. ⏰") }
        chip3.setOnClickListener { etMsg.setText("Thank you for contacting the library. Is there anything else I can help you with? 😊") }

        btnHistory.setOnClickListener { showHistoryDialog() }

        // Listen for student typing
        typingListener = db.collection("helpdesk_threads").document(threadId)
            .addSnapshotListener { snap, _ ->
                val studentTyping = snap?.getBoolean("studentTyping") ?: false
                tvTyping.visibility = if (studentTyping) View.VISIBLE else View.GONE
                tvTyping.text = "🎓 $studentName is typing..."
            }

        db.collection("helpdesk_threads").document(threadId)
            .update("unreadStaff", false)

        listener = db.collection("helpdesk_threads").document(threadId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                messages.clear()
                for (doc in snap.documents) messages.add(doc.data ?: continue)
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) rv.smoothScrollToPosition(messages.size - 1)
            }
    }

    private fun showHistoryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_bot_history, null)
        val tvHistory  = dialogView.findViewById<TextView>(R.id.tvBotHistory)
        val sb = StringBuilder()
        messages.forEach { msg ->
            val role = msg["senderRole"]?.toString() ?: "?"
            val name = msg["senderName"]?.toString() ?: ""
            val text = msg["text"]?.toString() ?: ""
            val ts   = msg["timestamp"] as? Timestamp
            val time = if (ts != null) java.text.SimpleDateFormat("dd MMM hh:mm a", java.util.Locale.getDefault()).format(ts.toDate()) else ""
            val prefix = when (role) {
                "bot"     -> "🤖 Bot"
                "system"  -> "⚙️ System"
                "staff"   -> "👤 $name"
                "student" -> "🎓 $name"
                else      -> name
            }
            sb.appendLine("[$time] $prefix:")
            sb.appendLine(text)
            sb.appendLine()
        }
        tvHistory.text = sb.toString().ifEmpty { "No messages yet." }
        android.app.AlertDialog.Builder(this)
            .setTitle("📜 Conversation History")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener?.remove()
        typingListener?.remove()
        // Clear typing flag
        db.collection("helpdesk_threads").document(threadId)
            .update("staffTyping", false)
    }
}
