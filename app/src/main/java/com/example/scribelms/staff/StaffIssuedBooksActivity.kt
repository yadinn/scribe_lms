package com.example.scribelms.staff

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.StaffIssuedBooksAdapter
import com.google.firebase.firestore.FirebaseFirestore

class StaffIssuedBooksActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: StaffIssuedBooksAdapter
    private lateinit var tvEmpty: TextView
    private val list = mutableListOf<Map<String, Any>>()
    private val db   = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_issued_books)

        rv      = findViewById(R.id.rvIssuedBooks)
        tvEmpty = findViewById<TextView?>(R.id.tvEmpty) ?: TextView(this)

        rv.layoutManager = LinearLayoutManager(this)
        adapter = StaffIssuedBooksAdapter(list)
        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadIssuedBooks()
    }

    private fun loadIssuedBooks() {
        db.collection("issue_requests")
            .whereEqualTo("status", "issued")
            .get()
            .addOnSuccessListener { snap ->
                list.clear()
                for (doc in snap.documents) {
                    list.add(doc.data!! + mapOf("requestId" to doc.id))
                }
                adapter.notifyDataSetChanged()
            }
    }
}
