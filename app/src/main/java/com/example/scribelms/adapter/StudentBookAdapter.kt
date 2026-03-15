package com.example.scribelms.adapter

import android.content.Intent
import android.graphics.Color
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scribelms.R
import com.example.scribelms.student.StudentBookDetailsActivity

class StudentBookAdapter(
    private val books: List<Map<String, Any>>
) : RecyclerView.Adapter<StudentBookAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val iv:      ImageView = v.findViewById(R.id.ivBookImage)
        val tvTitle: TextView  = v.findViewById(R.id.tvTitle)
        val tvAuthor:TextView  = v.findViewById(R.id.tvAuthor)
        val tvAvail: TextView  = v.findViewById(R.id.tvAvailability)
        val tvShelf: TextView  = v.findViewById(R.id.tvShelf)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_student_book, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val b     = books[pos]
        val avail = (b["availableCopies"] as? Long)?.toInt() ?: 0
        val total = (b["totalCopies"]     as? Long)?.toInt() ?: avail

        h.tvTitle.text  = "📚 ${b["title"]?.toString() ?: "Book"}"
        h.tvAuthor.text = "✍️ ${b["author"]?.toString() ?: "-"}"
        h.tvAvail.text  = "$avail/$total"
        h.tvAvail.setBackgroundColor(
            if (avail > 0) Color.parseColor("#388E3C") else Color.parseColor("#D32F2F")
        )

        val shelf = b["shelfLocation"]?.toString()
        if (!shelf.isNullOrBlank()) {
            h.tvShelf.visibility = View.VISIBLE
            h.tvShelf.text = "📍 $shelf"
        } else h.tvShelf.visibility = View.GONE

        Glide.with(h.itemView.context)
            .load(b["imageUrl"] as? String)
            .placeholder(R.drawable.ic_book_placeholder)
            .error(R.drawable.ic_book_placeholder)
            .centerCrop().into(h.iv)

        val id = b["id"] as? String ?: return
        h.itemView.setOnClickListener {
            h.itemView.context.startActivity(
                Intent(h.itemView.context, StudentBookDetailsActivity::class.java)
                    .putExtra("bookId", id)
            )
        }
    }

    override fun getItemCount() = books.size
}
