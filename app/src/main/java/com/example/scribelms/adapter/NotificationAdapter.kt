package com.example.scribelms.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R

class NotificationAdapter(
    private val list: List<Pair<String, Map<String, Any>>>,
    private val onRead: (String) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle: TextView = v.findViewById(R.id.tvNotifTitle)
        val tvMsg:   TextView = v.findViewById(R.id.tvNotifMsg)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_notification, p, false))

    override fun onBindViewHolder(h: VH, i: Int) {
        val (id, data) = list[i]
        val read  = data["read"] as? Boolean ?: false
        val title = data["title"]?.toString() ?: ""

        // Pick emoji based on notification title keywords
        val emoji = when {
            title.contains("issued",   ignoreCase = true) -> "📗"
            title.contains("return",   ignoreCase = true) -> "📦"
            title.contains("fine",     ignoreCase = true) -> "💰"
            title.contains("approved", ignoreCase = true) -> "✅"
            title.contains("rejected", ignoreCase = true) -> "❌"
            title.contains("overdue",  ignoreCase = true) -> "⏰"
            title.contains("paid",     ignoreCase = true) -> "💳"
            else                                          -> "🔔"
        }

        h.tvTitle.text = "$emoji $title"
        h.tvMsg.text   = data["message"]?.toString() ?: ""
        h.tvTitle.setTypeface(null, if (read) Typeface.NORMAL else Typeface.BOLD)
        h.itemView.setOnClickListener { onRead(id) }
    }

    override fun getItemCount(): Int = list.size
}
