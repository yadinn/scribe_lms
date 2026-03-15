package com.example.scribelms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class IssueHistoryAdapter(
    private val list: List<Map<String, Any>>
) : RecyclerView.Adapter<IssueHistoryAdapter.VH>() {

    private val db = FirebaseFirestore.getInstance()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvBookTitle:    TextView = v.findViewById(R.id.tvBookTitle)
        val tvStudentName:  TextView = v.findViewById(R.id.tvStudentName)
        val tvStudentRegNo: TextView = v.findViewById(R.id.tvStudentRegNo)
        val tvContact:      TextView = v.findViewById(R.id.tvContact)
        val tvStatus:       TextView = v.findViewById(R.id.tvStatus)
        val tvDates:        TextView = v.findViewById(R.id.tvDates)
        val tvFine:         TextView = v.findViewById(R.id.tvFine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_issue_history, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val item   = list[position]
        val status = (item["status"] ?: "--").toString()

        h.tvBookTitle.text = "📚 ${item["bookTitle"] ?: "--"}"

        val statusEmoji = when (status.lowercase()) {
            "pending"  -> "🕐"
            "approved" -> "✅"
            "issued"   -> "📖"
            "returned" -> "📦"
            "rejected" -> "❌"
            "expired"  -> "⏰"
            else       -> "❓"
        }
        h.tvStatus.text = "$statusEmoji Status: ${status.uppercase()}"

        fun toDate(v: Any?): Date? = when (v) {
            is Timestamp -> v.toDate()
            is Date      -> v
            else         -> null
        }

        val fmt     = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val issued  = toDate(item["issuedAt"])?.let    { fmt.format(it) } ?: "--"
        val due     = toDate(item["returnDueDate"])?.let { fmt.format(it) } ?: "--"
        val returned = toDate(item["returnedAt"])?.let { fmt.format(it) } ?: "--"
        h.tvDates.text = "📅 Issued: $issued\n⏰ Due: $due\n📦 Returned: $returned"

        h.tvStudentName.text  = "👤 --"
        h.tvStudentRegNo.text = "🎓 Reg No: --"
        h.tvContact.text      = "📞 Contact: --"

        val studentUid = item["studentUid"] as? String
        if (studentUid != null) {
            db.collection("users").document(studentUid).get()
                .addOnSuccessListener { doc ->
                    if (!doc.exists()) return@addOnSuccessListener
                    val name    = doc.get("name")?.toString() ?: "--"
                    val regNo   = doc.get("registerNo")?.toString() ?: "--"
                    val contact = when (val c = doc.get("contact")) {
                        is String -> c
                        is Number -> c.toString()
                        else      -> "--"
                    }
                    h.tvStudentName.text  = "👤 $name"
                    h.tvStudentRegNo.text = "🎓 $regNo"
                    h.tvContact.text      = "📞 $contact"
                }
        }

        val lateDays   = (item["lateDays"]   as? Long)?.toInt() ?: (item["lateDays"]   as? Int) ?: 0
        val fineAmount = (item["fineAmount"] as? Long)?.toInt() ?: (item["fineAmount"] as? Int) ?: 0
        val finePaid   = item["finePaid"] as? Boolean ?: false

        h.tvFine.text = if (lateDays > 0 && fineAmount > 0) {
            val s = if (finePaid) "✅ PAID" else "⚠️ NOT PAID"
            "🔴 Late: $lateDays day(s)  |  💰 Fine: ₹$fineAmount  |  $s"
        } else "✅ No fine"
    }

    override fun getItemCount(): Int = list.size
}
