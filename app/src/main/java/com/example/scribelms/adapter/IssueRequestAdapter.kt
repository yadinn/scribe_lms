package com.example.scribelms.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.firestore.FirebaseFirestore

class IssueRequestAdapter(
    private val requestList: List<Map<String, Any>>,
    private val onApproveClick: (Map<String, Any>) -> Unit,
    private val onRejectClick: (Map<String, Any>) -> Unit,
    private val onCollectedClick: (Map<String, Any>) -> Unit,
    private val onReturnApproveClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<IssueRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBook:         TextView = view.findViewById(R.id.tvBookId)
        val tvStudent:      TextView = view.findViewById(R.id.tvStudentUid)
        val tvStudentRegNo: TextView = view.findViewById(R.id.tvStudentRegNo)
        val tvStatus:       TextView = view.findViewById(R.id.tvStatus)
        val btnApprove:     Button   = view.findViewById(R.id.btnApprove)
        val btnReject:      Button   = view.findViewById(R.id.btnReject)
        val btnCollected:   Button   = view.findViewById(R.id.btnCollected)
        val btnReturnApprove: Button = view.findViewById(R.id.btnReturnApprove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_issue_request, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val req    = requestList[position]
        val status = req["status"] as? String ?: ""
        val returnRequested = req["returnRequested"] as? Boolean ?: false

        holder.tvBook.text   = "📖 Book: ${req["bookTitle"] ?: "--"}"
        holder.tvStudent.text = "👤 Student: --"
        holder.tvStudentRegNo.text = "🎓 Reg No: --"

        // Status badge with emoji
        val statusEmoji = when (status) {
            "pending"  -> "🕐"
            "approved" -> "✅"
            "issued"   -> "📗"
            "returned" -> "📦"
            "rejected" -> "❌"
            else       -> "❓"
        }
        holder.tvStatus.text = "$statusEmoji Status: ${status.replaceFirstChar { it.uppercase() }}"

        val studentUid = req["studentUid"] as? String
        if (studentUid != null) {
            FirebaseFirestore.getInstance()
                .collection("users").document(studentUid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        holder.tvStudent.text      = "👤 ${doc.getString("name") ?: "--"}"
                        holder.tvStudentRegNo.text = "🎓 ${doc.getString("registerNo") ?: "--"}"
                    }
                }
        }

        holder.btnApprove.visibility      = View.GONE
        holder.btnReject.visibility       = View.GONE
        holder.btnCollected.visibility    = View.GONE
        holder.btnReturnApprove.visibility = View.GONE

        when {
            status == "pending" -> {
                holder.btnApprove.visibility = View.VISIBLE
                holder.btnReject.visibility  = View.VISIBLE
            }
            status == "approved" -> {
                holder.btnCollected.visibility = View.VISIBLE
            }
            status == "issued" && returnRequested -> {
                holder.btnReturnApprove.visibility = View.VISIBLE
            }
        }

        holder.btnApprove.setOnClickListener  { onApproveClick(req) }
        holder.btnReject.setOnClickListener   { onRejectClick(req) }
        holder.btnCollected.setOnClickListener { onCollectedClick(req) }
        holder.btnReturnApprove.setOnClickListener {
            val fine = (req["fineAmount"] as? Long ?: 0) > 0
            val paid = req["finePaid"] as? Boolean ?: false
            if (fine && !paid) {
                Toast.makeText(
                    holder.itemView.context,
                    "⚠️ Fine not paid. Collect fine before approving return.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            onReturnApproveClick(req)
        }
    }

    override fun getItemCount(): Int = requestList.size
}
