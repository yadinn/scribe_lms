package com.example.scribelms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.firestore.DocumentSnapshot

class StudentSingleReviewAdapter(
    private val doc: DocumentSnapshot,
    private val onDelete: () -> Unit
) : RecyclerView.Adapter<StudentSingleReviewAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvRating:  TextView = v.findViewById(R.id.tvRating)
        val tvComment: TextView = v.findViewById(R.id.tvComment)
        val btnDelete: Button   = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.row_student_single_review, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val rating  = doc.getLong("rating") ?: 0
        val comment = doc.getString("comment") ?: ""
        val stars   = "⭐".repeat(rating.toInt()) + "☆".repeat((5 - rating.toInt()).coerceAtLeast(0))
        h.tvRating.text  = stars
        h.tvComment.text = "💬 $comment"
        h.btnDelete.setOnClickListener { onDelete() }
    }

    override fun getItemCount(): Int = 1
}
