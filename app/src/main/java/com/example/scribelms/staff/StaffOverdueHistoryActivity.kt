package com.example.scribelms.staff

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StaffOverdueHistoryActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val list = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_overdue_history)

        val rv = findViewById<RecyclerView>(R.id.rvOverdueHistory)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter(list)
        rv.adapter = adapter
        loadHistory()
    }

    private fun loadHistory() {
        db.collection("issue_requests")
            .whereEqualTo("status", "returned")
            .get()
            .addOnSuccessListener { snap ->
                list.clear()
                for (doc in snap) {
                    val fine = (doc.getLong("fineAmount") ?: 0).toInt()
                    val late = (doc.getLong("lateDays") ?: 0).toInt()
                    if (late > 0 || fine > 0) {
                        list.add(doc.data + mapOf("id" to doc.id))
                    }
                }
                adapter.notifyDataSetChanged()
                if (list.isEmpty()) Toast.makeText(this, "No overdue history", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show()
            }
    }

    inner class HistoryAdapter(private val data: List<Map<String, Any>>) :
        RecyclerView.Adapter<HistoryAdapter.VH>() {

        private val db2 = FirebaseFirestore.getInstance()
        private val cache = mutableMapOf<String, String>()
        private val df = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvBook: TextView = v.findViewById(R.id.tvBook)
            val tvStudent: TextView = v.findViewById(R.id.tvStudent)
            val tvReturnedAt: TextView = v.findViewById(R.id.tvReturnedAt)
            val tvFine: TextView = v.findViewById(R.id.tvFine)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int) =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_overdue_history, p, false))

        override fun onBindViewHolder(h: VH, pos: Int) {
            val item = data[pos]
            val bookId = item["bookId"] as? String ?: ""
            val studentUid = item["studentUid"] as? String ?: ""
            val fine = (item["fineAmount"] as? Long)?.toInt() ?: 0
            val late = (item["lateDays"] as? Long)?.toInt() ?: 0
            val paid = item["finePaid"] as? Boolean ?: false

            // Book title
            if (bookId.isNotEmpty()) {
                db2.collection("books").document(bookId).get().addOnSuccessListener { doc ->
                    h.tvBook.text = "📚 ${doc.getString("title") ?: "--"}"
                }
            }

            // Student name
            h.tvStudent.text = "👤 --"
            if (studentUid.isNotEmpty()) {
                val cached = cache[studentUid]
                if (cached != null) {
                    h.tvStudent.text = "👤 $cached"
                } else {
                    db2.collection("users").document(studentUid).get().addOnSuccessListener { doc ->
                        val name = "${doc.getString("name") ?: "--"} (${doc.getString("registerNo") ?: "--"})"
                        cache[studentUid] = name
                        h.tvStudent.text = "👤 $name"
                    }
                }
            }

            // Returned date
            val returnedAt = when (val r = item["returnedAt"]) {
                is Timestamp -> r.toDate()
                is Date -> r
                else -> null
            }
            h.tvReturnedAt.text = "📅 Returned: ${returnedAt?.let { df.format(it) } ?: "--"}"

            // Fine
            if (fine > 0) {
                val paidStr = if (paid) "PAID ✅" else "UNPAID ❌"
                h.tvFine.text = "⚠️ Fine: ₹$fine ($late day(s)) — $paidStr"
                h.tvFine.setTextColor(if (paid) 0xFF388E3C.toInt() else 0xFFD32F2F.toInt())
            } else {
                h.tvFine.text = "✅ No fine"
                h.tvFine.setTextColor(0xFF388E3C.toInt())
            }
        }

        override fun getItemCount() = data.size
    }
}
