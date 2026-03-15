package com.example.scribelms.helpdesk

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class ThreadAdapter(
    private val threads: List<Map<String, Any>>,
    private val onClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<ThreadAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvName:    TextView = v.findViewById(R.id.tvStudentName)
        val tvLast:    TextView = v.findViewById(R.id.tvLastMessage)
        val tvTime:    TextView = v.findViewById(R.id.tvTime)
        val tvUnread:  TextView = v.findViewById(R.id.tvUnread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_helpdesk_thread, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t      = threads[position]
        val unread = t["unreadStaff"] as? Boolean ?: false

        holder.tvName.text = "👤 ${t["studentName"]?.toString() ?: "Student"}"
        holder.tvLast.text = t["lastMessage"]?.toString() ?: "No messages yet"

        val ts = t["updatedAt"] as? Timestamp
        if (ts != null) {
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            holder.tvTime.text = sdf.format(ts.toDate())
        }

        holder.tvUnread.visibility = if (unread) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick(t) }
    }

    override fun getItemCount() = threads.size
}
