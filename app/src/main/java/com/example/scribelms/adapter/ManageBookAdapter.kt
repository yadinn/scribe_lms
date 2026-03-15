package com.example.scribelms.adapter

import android.graphics.Color
import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scribelms.R

class ManageBookAdapter(
    private val books: List<Map<String, Any>>,
    private val onClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<ManageBookAdapter.VH>() {

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_manage_book, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val b     = books[pos]
        val total = (b["totalCopies"]     as? Long ?: 0).toInt()
        val avail = (b["availableCopies"] as? Long ?: 0).toInt()

        h.tvTitle.text  = "📚 ${b["title"]?.toString() ?: "-"}"
        h.tvAuthor.text = "✍️ ${b["author"]?.toString() ?: "-"}"
        h.tvCopies.text = "📋 Available: $avail / $total"

        if (avail > 0) {
            h.tvStatus.text = "✅ Available"
            h.tvStatus.setTextColor(Color.parseColor("#388E3C"))
        } else {
            h.tvStatus.text = "❌ Not Available"
            h.tvStatus.setTextColor(Color.parseColor("#D32F2F"))
        }

        Glide.with(h.itemView.context)
            .load(b["imageUrl"] as? String)
            .placeholder(R.drawable.ic_book_placeholder)
            .error(R.drawable.ic_book_placeholder)
            .into(h.iv)

        h.itemView.setOnClickListener { onClick(b) }
    }

    override fun getItemCount() = books.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val iv:       ImageView = v.findViewById(R.id.ivBookImage)
        val tvTitle:  TextView  = v.findViewById(R.id.tvTitle)
        val tvAuthor: TextView  = v.findViewById(R.id.tvAuthor)
        val tvCopies: TextView  = v.findViewById(R.id.tvCopies)
        val tvStatus: TextView  = v.findViewById(R.id.tvStatus)
    }
}
