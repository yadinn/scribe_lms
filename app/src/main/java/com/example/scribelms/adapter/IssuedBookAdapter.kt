package com.example.scribelms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

class IssuedBookAdapter(
    private val issuedList: List<Map<String, Any>>,
    private val onReturnClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<IssuedBookAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvBookTitle: TextView  = v.findViewById(R.id.tvBookTitle)
        val btnReturn:   Button    = v.findViewById(R.id.btnReturn)
        val tvIssuedDate: TextView? = v.findViewById(R.id.tvIssuedDate)
        val tvDates:      TextView? = v.findViewById(R.id.tvDates)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_issued_book, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item    = issuedList[pos]
        val dateFmt = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeFmt = SimpleDateFormat("hh:mm a",     Locale.getDefault())

        h.tvBookTitle.text = "📚 ${item["bookTitle"]?.toString() ?: "Unknown Book"}"

        fun toDate(v: Any?): Date? = when (v) {
            is Timestamp -> v.toDate()
            is Date      -> v
            else         -> null
        }

        val issuedAt = toDate(item["issuedAt"])
        h.tvIssuedDate?.text = if (issuedAt != null)
            "📅 Issued: ${dateFmt.format(issuedAt)}"
        else "📅 Issued: --"

        val due = toDate(item["returnDueDate"])
        h.tvDates?.text = if (due != null)
            "⏰ Due: ${dateFmt.format(due)} ${timeFmt.format(due)}"
        else "⏰ Due: --"

        val status = item["status"]?.toString() ?: ""
        if (status == "issued") {
            h.btnReturn.visibility = View.VISIBLE
            h.btnReturn.setOnClickListener { onReturnClick(item) }
        } else {
            h.btnReturn.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = issuedList.size
}
