package com.example.scribelms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R

class ResourceAdapter(
    private val list: List<Map<String, Any>>,
    private val isStaff: Boolean,
    private val onOpen: (String) -> Unit,
    private val onDelete: (String) -> Unit = {}
) : RecyclerView.Adapter<ResourceAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvTitle:     TextView    = v.findViewById(R.id.tvTitle)
        val tvDesc:      TextView    = v.findViewById(R.id.tvDescription)
        val btnDownload: Button      = v.findViewById(R.id.btnDownload)
        val btnDelete:   ImageButton = v.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_resource, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item  = list[pos]
        val title = item["title"]?.toString() ?: ""
        val desc  = item["description"]?.toString() ?: ""
        val url   = item["url"]?.toString() ?: ""
        val id    = item["id"]?.toString() ?: ""

        // Pick icon based on title/url extension
        val icon = when {
            url.contains(".pdf",  ignoreCase = true) -> "📄"
            url.contains(".ppt",  ignoreCase = true) -> "📊"
            url.contains(".doc",  ignoreCase = true) -> "📝"
            url.contains(".xls",  ignoreCase = true) -> "📈"
            url.contains(".mp4",  ignoreCase = true) -> "🎬"
            url.contains(".zip",  ignoreCase = true) -> "🗜️"
            else                                     -> "📎"
        }

        h.tvTitle.text = "$icon $title"
        h.tvDesc.text  = desc

        h.btnDownload.setOnClickListener { if (url.isNotEmpty()) onOpen(url) }
        h.itemView.setOnClickListener   { if (url.isNotEmpty()) onOpen(url) }

        if (isStaff) {
            h.btnDelete.visibility = View.VISIBLE
            h.btnDelete.setOnClickListener { if (id.isNotEmpty()) onDelete(id) }
        } else {
            h.btnDelete.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = list.size
}
