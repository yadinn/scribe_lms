package com.example.scribelms.adapter

import android.view.*
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scribelms.R

class StaffEBooksAdapter(
    private val list: List<Map<String, Any>>,
    private val onEdit:   (Map<String, Any>) -> Unit,
    private val onDelete: (String) -> Unit
) : RecyclerView.Adapter<StaffEBooksAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val iv:     ImageView = v.findViewById(R.id.ivCover)
        val title:  TextView  = v.findViewById(R.id.tvTitle)
        val author: TextView  = v.findViewById(R.id.tvAuthor)
        val cat:    TextView  = v.findViewById(R.id.tvCategory)
        val edit:   Button    = v.findViewById(R.id.btnEdit)
        val del:    Button    = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(LayoutInflater.from(p.context).inflate(R.layout.row_staff_ebook, p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val e = list[i]
        h.title.text  = "📱 ${e["title"]}"
        h.author.text = "✍️ ${e["author"]}"
        h.cat.text    = "🏷️ ${e["category"]}"
        Glide.with(h.itemView.context).load(e["imageUrl"]).into(h.iv)
        h.edit.setOnClickListener { onEdit(e) }
        h.del.setOnClickListener  { onDelete(e["id"].toString()) }
    }

    override fun getItemCount() = list.size
}
