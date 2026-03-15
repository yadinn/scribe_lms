package com.example.scribelms.student

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class StudentRequestStatusActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: StudentRequestStatusAdapter
    private val list = mutableListOf<Map<String, Any>>()
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_request_status)

        rv = findViewById(R.id.rvRequestStatus)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = StudentRequestStatusAdapter(list)
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Real-time listener – updates automatically when status changes
        listener = FirebaseFirestore.getInstance()
            .collection("issue_requests")
            .whereEqualTo("studentUid", uid)
            .addSnapshotListener { snap, _ ->
                if (snap == null) return@addSnapshotListener
                list.clear()
                for (doc in snap.documents) {
                    list.add(doc.data!! + mapOf("requestId" to doc.id))
                }
                adapter.notifyDataSetChanged()
            }
    }

    override fun onPause() {
        super.onPause()
        listener?.remove()
        listener = null
    }
}
