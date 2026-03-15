package com.example.scribelms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class StaffIssuedBooksAdapter(
    private val list: List<Map<String, Any>>
) : RecyclerView.Adapter<StaffIssuedBooksAdapter.VH>() {

    private val db = FirebaseFirestore.getInstance()
    private val studentCache = mutableMapOf<String, Pair<String, String>>()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvBook:      TextView       = v.findViewById(R.id.tvBookTitle)
        val tvStudent:   TextView       = v.findViewById(R.id.tvStudentName)
        val tvRegNo:     TextView       = v.findViewById(R.id.tvStudentRegNo)
        val tvDates:     TextView       = v.findViewById(R.id.tvDates)
        val tvFine:      TextView       = v.findViewById(R.id.tvFine)
        val btnMarkPaid: MaterialButton = v.findViewById(R.id.btnMarkPaid)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_staff_issued_book, parent, false))

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = list[position]

        h.tvBook.text    = "📚 ${item["bookTitle"] ?: "--"}"
        h.tvStudent.text = "👤 Student: --"
        h.tvRegNo.text   = "🎓 Reg No: --"

        val studentUid = item["studentUid"] as? String
        if (studentUid != null) {
            val cached = studentCache[studentUid]
            if (cached != null) {
                h.tvStudent.text = "👤 ${cached.first}"
                h.tvRegNo.text   = "🎓 ${cached.second}"
            } else {
                db.collection("users").document(studentUid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val name = doc.getString("name") ?: "--"
                            val reg  = doc.getString("registerNo") ?: "--"
                            studentCache[studentUid] = Pair(name, reg)
                            h.tvStudent.text = "👤 $name"
                            h.tvRegNo.text   = "🎓 $reg"
                        }
                    }
            }
        }

        fun toDate(v: Any?): Date? = when (v) {
            is Timestamp -> v.toDate()
            is Date      -> v
            else         -> null
        }

        val df     = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val issued = toDate(item["issuedAt"])
        val due    = toDate(item["returnDueDate"])
        h.tvDates.text = "📅 Issued: ${issued?.let { df.format(it) } ?: "--"}   ⏰ Due: ${due?.let { df.format(it) } ?: "--"}"

        val fine = (item["fineAmount"] as? Long)?.toInt() ?: 0
        val late = (item["lateDays"]   as? Long)?.toInt() ?: 0
        val paid = item["finePaid"] as? Boolean ?: false

        if (fine > 0) {
            h.tvFine.visibility      = View.VISIBLE
            h.btnMarkPaid.visibility = View.VISIBLE
            if (paid) {
                h.tvFine.text = "✅ Fine: Rs.$fine ($late day(s)) — PAID"
                h.tvFine.setTextColor(0xFF388E3C.toInt())
                h.btnMarkPaid.text      = "✅ Paid"
                h.btnMarkPaid.isEnabled = false
            } else {
                h.tvFine.text = "⚠️ Fine: Rs.$fine ($late day(s) late)"
                h.tvFine.setTextColor(0xFFD32F2F.toInt())
                h.btnMarkPaid.text      = "💳 Mark Fine Paid"
                h.btnMarkPaid.isEnabled = true
                h.btnMarkPaid.setOnClickListener {
                    db.collection("issue_requests")
                        .document(item["requestId"] as String)
                        .update("finePaid", true)
                        .addOnSuccessListener {
                            h.tvFine.text = "✅ Fine: Rs.$fine ($late day(s)) — PAID"
                            h.tvFine.setTextColor(0xFF388E3C.toInt())
                            h.btnMarkPaid.text      = "✅ Paid"
                            h.btnMarkPaid.isEnabled = false
                        }
                }
            }
        } else {
            // No fine — hide both, no text shown
            h.tvFine.visibility      = View.GONE
            h.btnMarkPaid.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = list.size
}
