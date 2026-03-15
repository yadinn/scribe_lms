package com.example.scribelms.student

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StudentBookHistoryActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var rv:      RecyclerView
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_book_history)
        rv      = findViewById(R.id.rvHistory)
        tvEmpty = findViewById(R.id.tvEmpty)
        rv.layoutManager = LinearLayoutManager(this)
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        val uid = auth.uid ?: return
        db.collection("issue_requests").whereEqualTo("studentUid", uid).get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    tvEmpty.visibility = View.VISIBLE; rv.visibility = View.GONE; return@addOnSuccessListener
                }
                tvEmpty.visibility = View.GONE; rv.visibility = View.VISIBLE
                rv.adapter = HistoryAdapter(snap.documents.sortedByDescending { it.getDate("requestedAt") })
            }
            .addOnFailureListener { tvEmpty.text = "❌ Failed to load"; tvEmpty.visibility = View.VISIBLE }
    }

    inner class HistoryAdapter(
        private val docs: List<com.google.firebase.firestore.DocumentSnapshot>
    ) : RecyclerView.Adapter<HistoryAdapter.VH>() {

        private val fmt = SimpleDateFormat("dd MMM yyyy  hh:mm a", Locale.getDefault())

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvTitle     = v.findViewById<TextView>(R.id.tvBookTitle)
            val tvStatus    = v.findViewById<TextView>(R.id.tvStatus)
            val tvRequested = v.findViewById<TextView>(R.id.tvRequestedAt)
            val tvIssued    = v.findViewById<TextView>(R.id.tvIssuedAt)
            val tvDue       = v.findViewById<TextView>(R.id.tvDueDate)
            val tvReturned  = v.findViewById<TextView>(R.id.tvReturnedAt)
            val tvFine      = v.findViewById<TextView>(R.id.tvFine)
        }

        override fun onCreateViewHolder(parent: ViewGroup, vt: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_book_history, parent, false))

        override fun getItemCount() = docs.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val doc    = docs[pos]
            val status = doc.getString("status") ?: "pending"

            h.tvTitle.text = "📚 ${doc.getString("bookTitle") ?: "-"}"

            val (label, color) = when (status) {
                "pending"  -> "🕐 Pending"  to "#FF9800"
                "approved" -> "✅ Approved" to "#4CAF50"
                "issued"   -> "📖 Issued"   to "#1976D2"
                "returned" -> "📦 Returned" to "#388E3C"
                "rejected" -> "❌ Rejected" to "#D32F2F"
                "expired"  -> "⏰ Expired"  to "#757575"
                else       -> status        to "#9E9E9E"
            }
            h.tvStatus.text = label
            h.tvStatus.setBackgroundColor(Color.parseColor(color))

            h.tvRequested.text = "📋 Requested: ${doc.getDate("requestedAt")?.let { fmt.format(it) } ?: "-"}"

            val issuedAt = doc.getDate("issuedAt")
            if (issuedAt != null) {
                h.tvIssued.visibility = View.VISIBLE
                h.tvIssued.text = "📅 Issued: ${fmt.format(issuedAt)}"
            } else h.tvIssued.visibility = View.GONE

            val dueDate = doc.getDate("returnDueDate")
            if (dueDate != null) {
                h.tvDue.visibility = View.VISIBLE
                val overdue = dueDate.before(Date()) && status == "issued"
                h.tvDue.text = "⏰ Due: ${fmt.format(dueDate)}${if (overdue) "  ⚠️ OVERDUE" else ""}"
                h.tvDue.setTextColor(if (overdue) Color.parseColor("#D32F2F") else Color.parseColor("#757575"))
            } else h.tvDue.visibility = View.GONE

            val returnedAt = doc.getDate("returnedAt")
            if (returnedAt != null) {
                h.tvReturned.visibility = View.VISIBLE
                h.tvReturned.text = "📦 Returned: ${fmt.format(returnedAt)}"
            } else h.tvReturned.visibility = View.GONE

            val fine     = doc.getLong("fineAmount") ?: 0L
            val lateDays = doc.getLong("lateDays")   ?: 0L
            val paid     = doc.getBoolean("finePaid") == true
            if (fine > 0) {
                h.tvFine.visibility = View.VISIBLE
                if (paid) {
                    h.tvFine.text = "✅ Fine ₹$fine ($lateDays day(s) late) — PAID"
                    h.tvFine.setTextColor(Color.parseColor("#388E3C"))
                } else {
                    h.tvFine.text = "⚠️ Fine ₹$fine ($lateDays day(s) late) — UNPAID"
                    h.tvFine.setTextColor(Color.parseColor("#D32F2F"))
                }
            } else h.tvFine.visibility = View.GONE
        }
    }
}
