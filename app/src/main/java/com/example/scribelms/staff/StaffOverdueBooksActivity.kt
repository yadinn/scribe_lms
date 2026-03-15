package com.example.scribelms.staff

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.StaffOverdueBooksAdapter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class StaffOverdueBooksActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: StaffOverdueBooksAdapter
    private val list = mutableListOf<Map<String, Any>>()

    private val db = FirebaseFirestore.getInstance()
    private var finePerDay = 0

    override fun onResume() {
        super.onResume()
        loadLibrarySettings()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_overdue_books)

        rv = findViewById(R.id.rvOverdue)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = StaffOverdueBooksAdapter(list)
        rv.adapter = adapter

        loadLibrarySettings()
    }

    // 🔹 Load finePerDay from settings/library
    private fun loadLibrarySettings() {
        db.collection("settings")
            .document("library")
            .get()
            .addOnSuccessListener {
                finePerDay = (it.getLong("finePerDay") ?: 0).toInt()
                loadOverdue()
            }
    }

    // 🔹 Load overdue books + calculate fine
    private fun loadOverdue() {
        val now = System.currentTimeMillis()

        db.collection("issue_requests")
            .whereEqualTo("status", "issued")
            .get()
            .addOnSuccessListener { result ->
                list.clear()

                for (doc in result) {
                    val dueTs = doc.getTimestamp("returnDueDate") ?: continue
                    val dueTime = dueTs.toDate().time

                    if (now > dueTime) {
                        val lateDays =
                            ((now - dueTime) / (1000 * 60 * 60 * 24)).toInt() + 1
                        val fine = lateDays * finePerDay

                        // 🔥 Update Firestore automatically
                        doc.reference.update(
                            mapOf(
                                "lateDays" to lateDays,
                                "fineAmount" to fine
                            )
                        )

                        list.add(doc.data + mapOf("requestId" to doc.id))
                    }
                }

                adapter.notifyDataSetChanged()
            }
    }
}