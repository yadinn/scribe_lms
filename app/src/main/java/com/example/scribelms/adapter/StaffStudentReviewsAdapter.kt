package com.example.scribelms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StaffStudentReviewsAdapter(
    private val list: MutableList<Map<String, Any>>,
    private val onDeleteDone: () -> Unit
) : RecyclerView.Adapter<StaffStudentReviewsAdapter.VH>() {

    private val db = FirebaseFirestore.getInstance()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvStudent: TextView = v.findViewById(R.id.tvStudentName)
        val tvBook:    TextView = v.findViewById(R.id.tvBookName)
        val tvRating:  TextView = v.findViewById(R.id.tvRating)
        val tvComment: TextView = v.findViewById(R.id.tvComment)
        val tvDate:    TextView = v.findViewById(R.id.tvDate)
        val btnDelete: Button   = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.row_staff_review, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val item   = list[position]
        val rating = (item["rating"] as? Long)?.toInt() ?: 0
        val stars  = "⭐".repeat(rating) + "☆".repeat((5 - rating).coerceAtLeast(0))

        h.tvStudent.text = "👤 ${item["studentName"]}"
        h.tvBook.text    = "📚 ${item["bookTitle"]}"
        h.tvRating.text  = stars
        h.tvComment.text = "💬 ${item["comment"]}"

        val date = item["updatedAt"] as? Date
        val fmt  = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        h.tvDate.text = if (date != null) "📅 ${fmt.format(date)}" else "📅 -"

        val docId = item["docId"].toString()
        h.btnDelete.setOnClickListener {
            db.collection("reviews").document(docId).delete()
                .addOnSuccessListener {
                    Toast.makeText(h.itemView.context, "Review deleted", Toast.LENGTH_SHORT).show()
                    onDeleteDone()
                }
        }
    }

    override fun getItemCount(): Int = list.size
}
