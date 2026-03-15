package com.example.scribelms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class StaffOverdueBooksAdapter(
    private val list: List<Map<String, Any>>
) : RecyclerView.Adapter<StaffOverdueBooksAdapter.VH>() {

    private val db = FirebaseFirestore.getInstance()
    private val studentCache = mutableMapOf<String, Triple<String, String, String>>()

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvBook:    TextView = v.findViewById(R.id.tvBookTitle)
        val tvStudent: TextView = v.findViewById(R.id.tvStudentName)
        val tvRegNo:   TextView = v.findViewById(R.id.tvStudentRegNo)
        val tvContact: TextView = v.findViewById(R.id.tvContact)
        val tvLate:    TextView = v.findViewById(R.id.tvLate)
        val tvFine:    TextView = v.findViewById(R.id.tvFine)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH =
        VH(LayoutInflater.from(p.context).inflate(R.layout.item_overdue_book, p, false))

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = list[pos]

        h.tvBook.text    = "📚 ${item["bookTitle"] ?: "--"}"
        h.tvStudent.text = "👤 Student: --"
        h.tvRegNo.text   = "🎓 Reg No: --"
        h.tvContact.text = "📞 Contact: --"

        val studentUid = item["studentUid"] as? String
        if (studentUid != null) {
            val cached = studentCache[studentUid]
            if (cached != null) {
                h.tvStudent.text = "👤 ${cached.first}"
                h.tvRegNo.text   = "🎓 ${cached.second}"
                h.tvContact.text = "📞 ${cached.third}"
            } else {
                db.collection("users").document(studentUid).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            val name    = doc.getString("name")    ?: "--"
                            val reg     = doc.getString("registerNo") ?: "--"
                            val contact = doc.getString("contact") ?: "--"
                            studentCache[studentUid] = Triple(name, reg, contact)
                            h.tvStudent.text = "👤 $name"
                            h.tvRegNo.text   = "🎓 $reg"
                            h.tvContact.text = "📞 $contact"
                        }
                    }
            }
        }

        fun toDate(v: Any?): Date? = when (v) {
            is Timestamp -> v.toDate()
            is Date      -> v
            else         -> null
        }

        val due = toDate(item["returnDueDate"])
        val lateDays = if (due != null)
            maxOf(((Date().time - due.time) / (1000 * 60 * 60 * 24)).toInt(), 1)
        else 0

        h.tvLate.text = "🔴 $lateDays day(s) late"

        val fine = (item["fineAmount"] as? Long)?.toInt()
            ?: (item["fineAmount"] as? Int) ?: 0

        if (fine > 0) {
            h.tvFine.visibility = View.VISIBLE
            h.tvFine.text = "⚠️ Fine: Rs.$fine"
        } else {
            h.tvFine.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = list.size
}
