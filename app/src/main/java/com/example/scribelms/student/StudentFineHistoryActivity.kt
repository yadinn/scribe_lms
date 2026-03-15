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

class StudentFineHistoryActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var rv:        RecyclerView
    private lateinit var tvEmpty:   TextView
    private lateinit var tvSummary: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_fine_history)
        rv        = findViewById(R.id.rvFineHistory)
        tvEmpty   = findViewById(R.id.tvEmpty)
        tvSummary = findViewById(R.id.tvSummary)
        rv.layoutManager = LinearLayoutManager(this)
        load()
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        val uid = auth.uid ?: return
        db.collection("issue_requests")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("status", "returned")
            .get()
            .addOnSuccessListener { snap ->
                val fined = snap.documents
                    .filter { (it.getLong("fineAmount") ?: 0) > 0 }
                    .sortedByDescending { it.getDate("returnedAt") }

                if (fined.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE; rv.visibility = View.GONE
                    tvSummary.text = "🎉 No fines on record!"
                    tvSummary.setTextColor(Color.parseColor("#388E3C"))
                    return@addOnSuccessListener
                }

                tvEmpty.visibility = View.GONE; rv.visibility = View.VISIBLE

                val total  = fined.sumOf { it.getLong("fineAmount") ?: 0L }
                val paid   = fined.filter { it.getBoolean("finePaid") == true }.sumOf { it.getLong("fineAmount") ?: 0L }
                val unpaid = total - paid
                tvSummary.text = "💰 Total ₹$total  |  ✅ Paid ₹$paid  |  ⚠️ Pending ₹$unpaid"
                tvSummary.setTextColor(if (unpaid > 0) Color.parseColor("#D32F2F") else Color.parseColor("#388E3C"))

                rv.adapter = FineAdapter(fined)
            }
    }

    inner class FineAdapter(
        private val docs: List<com.google.firebase.firestore.DocumentSnapshot>
    ) : RecyclerView.Adapter<FineAdapter.VH>() {

        private val fmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvBook   = v.findViewById<TextView>(R.id.tvBookTitle)
            val tvFine   = v.findViewById<TextView>(R.id.tvFineAmount)
            val tvStatus = v.findViewById<TextView>(R.id.tvFineStatus)
            val tvDate   = v.findViewById<TextView>(R.id.tvReturnDate)
        }

        override fun onCreateViewHolder(p: ViewGroup, vt: Int): VH =
            VH(LayoutInflater.from(p.context).inflate(R.layout.item_student_fine, p, false))

        override fun getItemCount() = docs.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val doc  = docs[pos]
            val fine = doc.getLong("fineAmount") ?: 0L
            val paid = doc.getBoolean("finePaid") == true
            val days = doc.getLong("lateDays") ?: 0L

            h.tvBook.text  = "📚 ${doc.getString("bookTitle") ?: "-"}"
            h.tvFine.text  = "💰 ₹$fine  ($days day(s) late)"
            h.tvDate.text  = "📅 ${doc.getDate("returnedAt")?.let { fmt.format(it) } ?: "-"}"

            if (paid) {
                h.tvStatus.text = "✅ PAID"
                h.tvStatus.setTextColor(Color.parseColor("#388E3C"))
            } else {
                h.tvStatus.text = "⚠️ UNPAID"
                h.tvStatus.setTextColor(Color.parseColor("#D32F2F"))
            }
        }
    }
}
