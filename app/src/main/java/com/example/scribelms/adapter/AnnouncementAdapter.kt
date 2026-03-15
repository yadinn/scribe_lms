package com.example.scribelms.adapter

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R

class AnnouncementAdapter(
    private val list: List<Map<String, Any>>,
    private val isStaff: Boolean,
    private val onLongClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<AnnouncementAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle:   TextView = v.findViewById(R.id.tvTitle)
        val tvMessage: TextView = v.findViewById(R.id.tvMessage)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_announcement, p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val a = list[i]
        h.tvTitle.text   = "📢 ${a["title"]?.toString() ?: ""}"
        h.tvMessage.text = a["message"]?.toString() ?: ""
        if (isStaff) h.itemView.setOnLongClickListener { onLongClick(a); true }
    }

    override fun getItemCount() = list.size
}
