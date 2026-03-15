package com.example.scribelms.staff

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.IssueHistoryAdapter
import com.google.firebase.firestore.FirebaseFirestore

class StaffHistoryActivity : AppCompatActivity() {

    private lateinit var rvHistory: RecyclerView
    private lateinit var adapter: IssueHistoryAdapter

    private val historyList = mutableListOf<Map<String, Any>>()
    private val db = FirebaseFirestore.getInstance()

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_history)

        rvHistory = findViewById(R.id.rvHistory)
        rvHistory.layoutManager = LinearLayoutManager(this)

        adapter = IssueHistoryAdapter(historyList)
        rvHistory.adapter = adapter

        loadHistory()
    }

    private fun loadHistory() {

        db.collection("issue_requests")
            .get()
            .addOnSuccessListener { snapshot ->

                historyList.clear()

                for (doc in snapshot.documents) {

                    val status = doc.getString("status") ?: ""

                    // ✅ HISTORY CONDITIONS
                    if (
                        status == "issued" ||
                        status == "returned" ||
                        status == "expired" ||
                        status == "rejected"
                    ) {
                        historyList.add(doc.data!! + mapOf("requestId" to doc.id))
                    }
                }

                if (historyList.isEmpty()) {
                    Toast.makeText(this, "No history found", Toast.LENGTH_SHORT).show()
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show()
            }
    }
}