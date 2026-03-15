package com.example.scribelms.staff

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StaffBookHistoryActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var rv: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_book_history)

        val bookId = intent.getStringExtra("bookId") ?: run { finish(); return }
        rv = findViewById(R.id.rvHistory)
        rv.layoutManager = LinearLayoutManager(this)

        db.collection("issue_requests")
            .whereEqualTo("bookId", bookId)
            .get()
            .addOnSuccessListener { snap ->
                val sorted = snap.documents.sortedByDescending { it.getDate("requestedAt") }
                rv.adapter = HistAdapter(sorted)
            }
    }

    inner class HistAdapter(
        private val docs: List<com.google.firebase.firestore.DocumentSnapshot>
    ) : RecyclerView.Adapter<HistAdapter.VH>() {

        private val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvStudent = v.findViewById<TextView>(R.id.tvStudentName)
            val tvStatus  = v.findViewById<TextView>(R.id.tvStatus)
            val tvDates   = v.findViewById<TextView>(R.id.tvDates)
            val tvFine    = v.findViewById<TextView>(R.id.tvFineInfo)
        }

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_staff_book_history, p, false))

        override fun getItemCount() = docs.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val doc    = docs[pos]
            val status = doc.getString("status") ?: "pending"
            val uid    = doc.getString("studentUid") ?: ""

            if (uid.isNotEmpty()) {
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { u -> h.tvStudent.text = "👤 ${u.getString("name") ?: uid}" }
                    .addOnFailureListener  { h.tvStudent.text = "👤 ${uid.take(8)}" }
            } else h.tvStudent.text = "👤 Unknown"

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

            val sb = StringBuilder()
            doc.getDate("requestedAt")?.let  { sb.appendLine("📋 Requested: ${fmt.format(it)}") }
            doc.getDate("issuedAt")?.let     { sb.appendLine("📅 Issued: ${fmt.format(it)}") }
            doc.getDate("returnDueDate")?.let { sb.appendLine("⏰ Due: ${fmt.format(it)}") }
            doc.getDate("returnedAt")?.let   { sb.appendLine("📦 Returned: ${fmt.format(it)}") }
            h.tvDates.text = sb.toString().trimEnd()

            val fine = doc.getLong("fineAmount") ?: 0L
            if (fine > 0) {
                val paid = doc.getBoolean("finePaid") == true
                h.tvFine.visibility = View.VISIBLE
                h.tvFine.text = if (paid) "✅ Fine ₹$fine — PAID" else "⚠️ Fine ₹$fine — UNPAID"
                h.tvFine.setTextColor(if (paid) Color.parseColor("#388E3C") else Color.parseColor("#D32F2F"))
            } else h.tvFine.visibility = View.GONE
        }
    }
}
