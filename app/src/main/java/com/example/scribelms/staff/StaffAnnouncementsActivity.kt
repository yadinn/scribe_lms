package com.example.scribelms.staff

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.AnnouncementAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class StaffAnnouncementsActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val list = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: AnnouncementAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_announcements)

        val rv     = findViewById<RecyclerView>(R.id.rvAnnouncements)
        val btnAdd = findViewById<Button>(R.id.btnAddAnnouncement)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = AnnouncementAdapter(list, true) { ann -> showOptions(ann) }
        rv.adapter = adapter
        btnAdd.setOnClickListener { showAddDialog(null) }
        load()
    }

    private fun load() {
        db.collection("announcements").whereEqualTo("active", true).get()
            .addOnSuccessListener { snap ->
                list.clear()
                for (d in snap) list.add(d.data + mapOf("id" to d.id))
                adapter.notifyDataSetChanged()
            }
    }

    private fun showAddDialog(existing: Map<String, Any>?) {
        val v       = layoutInflater.inflate(R.layout.dialog_add_announcement, null)
        val etTitle = v.findViewById<EditText>(R.id.etTitle)
        val etMsg   = v.findViewById<EditText>(R.id.etMessage)
        if (existing != null) {
            etTitle.setText(existing["title"]?.toString() ?: "")
            etMsg.setText(existing["message"]?.toString() ?: "")
        }
        val dialogTitle = if (existing != null) "✏️ Edit Announcement" else "📢 New Announcement"
        AlertDialog.Builder(this)
            .setTitle(dialogTitle).setView(v)
            .setPositiveButton("💾 Save") { _, _ ->
                val title = etTitle.text.toString().trim()
                val msg   = etMsg.text.toString().trim()
                if (title.isEmpty() || msg.isEmpty()) {
                    Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (existing != null) {
                    db.collection("announcements").document(existing["id"] as String)
                        .update(mapOf("title" to title, "message" to msg))
                        .addOnSuccessListener { Toast.makeText(this, "✅ Updated", Toast.LENGTH_SHORT).show(); load() }
                } else {
                    db.collection("announcements").add(mapOf(
                        "title" to title, "message" to msg,
                        "createdBy" to FirebaseAuth.getInstance().uid,
                        "createdAt" to Date(), "active" to true
                    )).addOnSuccessListener { Toast.makeText(this, "📢 Announcement added", Toast.LENGTH_SHORT).show(); load() }
                      .addOnFailureListener { Toast.makeText(this, it.message, Toast.LENGTH_LONG).show() }
                }
            }
            .setNegativeButton("Cancel", null).show()
    }

    private fun showOptions(ann: Map<String, Any>) {
        AlertDialog.Builder(this)
            .setTitle("📢 Options")
            .setItems(arrayOf("✏️ Edit", "🗑️ Delete")) { _, which ->
                when (which) { 0 -> showAddDialog(ann); 1 -> confirmDelete(ann) }
            }.show()
    }

    private fun confirmDelete(ann: Map<String, Any>) {
        AlertDialog.Builder(this)
            .setTitle("🗑️ Delete Announcement")
            .setMessage("Delete \"${ann["title"]}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("announcements").document(ann["id"] as String).delete()
                    .addOnSuccessListener { Toast.makeText(this, "🗑️ Deleted", Toast.LENGTH_SHORT).show(); load() }
            }
            .setNegativeButton("Cancel", null).show()
    }
}
