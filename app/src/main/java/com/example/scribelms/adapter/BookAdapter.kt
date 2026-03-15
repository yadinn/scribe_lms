package com.example.scribelms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.student.modeldata.Book

class BookAdapter(
    private val bookList: List<Book>,
    private val onRequestIssueClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle:        TextView = itemView.findViewById(R.id.tvBookTitle)
        val tvAuthor:       TextView = itemView.findViewById(R.id.tvBookAuthor)
        val tvCategory:     TextView = itemView.findViewById(R.id.tvBookCategory)
        val tvAvailability: TextView = itemView.findViewById(R.id.tvBookAvailability)
        val btnRequestIssue: Button  = itemView.findViewById(R.id.btnRequestIssue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder =
        BookViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false))

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookList[position]
        holder.tvTitle.text    = "📚 ${book.title}"
        holder.tvAuthor.text   = "✍️ Author: ${book.author}"
        holder.tvCategory.text = "🏷️ ${book.category}"

        if (book.availableCopies > 0) {
            holder.tvAvailability.text = "✅ Available (${book.availableCopies})"
            holder.tvAvailability.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
            holder.btnRequestIssue.isEnabled = true
        } else {
            holder.tvAvailability.text = "❌ Not Available"
            holder.tvAvailability.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
            holder.btnRequestIssue.isEnabled = false
        }
        holder.btnRequestIssue.setOnClickListener { onRequestIssueClick(book) }
    }

    override fun getItemCount(): Int = bookList.size
}
