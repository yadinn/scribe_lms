package com.example.scribelms.admin

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R

class UserAdapter(
    private val userList: List<Map<String, Any>>,
    private val onClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName:   TextView = itemView.findViewById(R.id.tvUserName)
        val tvRole:   TextView = itemView.findViewById(R.id.tvUserRole)
        val tvStatus: TextView = itemView.findViewById(R.id.tvUserStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder =
        UserViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false))

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user    = userList[position]
        val blocked = user["blocked"] as? Boolean ?: false
        val role    = user["role"]?.toString() ?: "unknown"

        val roleEmoji = when (role) {
            "admin"   -> "👑"
            "staff"   -> "🏫"
            "student" -> "🎓"
            else      -> "👤"
        }
        val roleName = role.replaceFirstChar { it.uppercase() }

        holder.tvName.text   = "👤 ${user["name"]?.toString() ?: "Unknown"}"
        holder.tvRole.text   = "$roleEmoji $roleName"
        holder.tvStatus.text = if (blocked) "🔒 BLOCKED" else "✅ ACTIVE"
        holder.tvStatus.setTextColor(
            if (blocked) Color.parseColor("#D32F2F") else Color.parseColor("#2E7D32")
        )
        holder.itemView.setOnClickListener { onClick(user) }
    }

    override fun getItemCount(): Int = userList.size
}
