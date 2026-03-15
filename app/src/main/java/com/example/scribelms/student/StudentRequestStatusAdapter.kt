package com.example.scribelms.student

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class StudentRequestStatusAdapter(
    private val list: List<Map<String, Any>>
) : RecyclerView.Adapter<StudentRequestStatusAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvBook:     TextView = v.findViewById(R.id.tvBook)
        val tvStatus:   TextView = v.findViewById(R.id.tvStatus)
        val tvIssued:   TextView = v.findViewById(R.id.tvIssued)
        val tvReturned: TextView = v.findViewById(R.id.tvReturned)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_student_request_status, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val item   = list[position]
        val status = item["status"]?.toString() ?: "--"
        val fmt    = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun toDate(v: Any?): Date? = when (v) {
            is Timestamp -> v.toDate()
            is Date      -> v
            else         -> null
        }

        val statusEmoji = when (status.lowercase()) {
            "pending"  -> "🕐"
            "approved" -> "✅"
            "issued"   -> "📖"
            "returned" -> "📦"
            "rejected" -> "❌"
            "expired"  -> "⏰"
            else       -> "❓"
        }

        h.tvBook.text     = "📚 ${item["bookTitle"]?.toString() ?: "--"}"
        h.tvStatus.text   = "$statusEmoji ${status.uppercase()}"
        h.tvIssued.text   = "📅 Issued: ${toDate(item["issuedAt"])?.let { fmt.format(it) } ?: "--"}"
        h.tvReturned.text = "📦 Returned: ${toDate(item["returnedAt"])?.let { fmt.format(it) } ?: "--"}"
    }

    override fun getItemCount(): Int = list.size
}
