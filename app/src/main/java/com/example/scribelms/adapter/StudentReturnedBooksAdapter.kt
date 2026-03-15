package com.example.scribelms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R

class StudentReturnedBooksAdapter(
    private val list: List<Map<String, Any>>,
    private val onReviewClick: (String) -> Unit
) : RecyclerView.Adapter<StudentReturnedBooksAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle:   TextView = v.findViewById(R.id.tvBookTitle)
        val btnReview: Button   = v.findViewById(R.id.btnReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.row_student_returned_book, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val item       = list[position]
        val title      = item["title"]?.toString() ?: "-"
        val bookId     = item["bookId"]?.toString() ?: return
        val hasReview  = item["hasReview"] as? Boolean ?: false

        h.tvTitle.text  = "📚 $title"
        h.btnReview.text = if (hasReview) "✏️ Edit Review" else "⭐ Add Review"
        h.btnReview.setOnClickListener { onReviewClick(bookId) }
    }

    override fun getItemCount(): Int = list.size
}
