package com.example.scribelms.helpdesk

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class StaffHelpdeskListActivity : AppCompatActivity() {

    private val db      = FirebaseFirestore.getInstance()
    private val threads = mutableListOf<Map<String, Any>>()
    private lateinit var adapter:  ThreadAdapter
    private lateinit var tvEmpty:  TextView
    private lateinit var tvCount:  TextView
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_helpdesk_list)

        tvEmpty = findViewById(R.id.tvEmpty)
        tvCount = findViewById(R.id.tvUnreadCount)
        val rv  = findViewById<RecyclerView>(R.id.rvThreads)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = ThreadAdapter(threads) { thread ->
            val studentUid  = thread["studentUid"]?.toString()  ?: return@ThreadAdapter
            val threadId    = "student_$studentUid"
            val studentName = thread["studentName"]?.toString() ?: "Student"
            startActivity(Intent(this, StaffHelpdeskChatActivity::class.java).apply {
                putExtra("threadId",    threadId)
                putExtra("studentUid",  studentUid)
                putExtra("studentName", studentName)
            })
        }
        rv.adapter = adapter

        listener = db.collection("helpdesk_threads")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                threads.clear()
                var unreadCount = 0
                for (doc in snap.documents) {
                    val data = (doc.data ?: continue).toMutableMap()
                    data["threadDocId"] = doc.id
                    if (data["unreadStaff"] as? Boolean == true) unreadCount++
                    if (!data.containsKey("studentName")) {
                        val uid = data["studentUid"]?.toString()
                        if (uid != null) {
                            db.collection("users").document(uid).get()
                                .addOnSuccessListener { u ->
                                    data["studentName"] = u.getString("name") ?: "Student"
                                    adapter.notifyDataSetChanged()
                                }
                        }
                    }
                    threads.add(data)
                }
                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (threads.isEmpty()) View.VISIBLE else View.GONE
                tvCount.text = if (unreadCount > 0) "🔴 $unreadCount unread" else "✅ All read"
                tvCount.visibility = View.VISIBLE
            }
    }

    override fun onDestroy() { super.onDestroy(); listener?.remove() }
}
