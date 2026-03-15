package com.example.scribelms.adapter

import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scribelms.R

class StudentEBooksAdapter(
    private val list: List<Map<String, Any>>,
    private val onClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<StudentEBooksAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivCover: ImageView = v.findViewById(R.id.ivCover)
        val tvTitle: TextView  = v.findViewById(R.id.tvTitle)
        val tvAuthor:TextView  = v.findViewById(R.id.tvAuthor)
        val tvCat:   TextView  = v.findViewById(R.id.tvCategory)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_student_ebook_grid, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = list[pos]
        h.tvTitle.text  = "📱 ${item["title"]?.toString()    ?: "-"}"
        h.tvAuthor.text = "✍️ ${item["author"]?.toString()   ?: "-"}"
        h.tvCat.text    = "🏷️ ${item["category"]?.toString() ?: ""}"
        Glide.with(h.itemView.context)
            .load(item["imageUrl"]?.toString())
            .placeholder(R.drawable.ic_book_placeholder)
            .error(R.drawable.ic_book_placeholder)
            .centerCrop().into(h.ivCover)
        h.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = list.size
}
