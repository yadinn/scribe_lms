package com.example.scribelms.staff

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.admin.UserAdapter
import com.google.firebase.firestore.FirebaseFirestore

class ViewStudentsActivity : AppCompatActivity() {

    private val db              = FirebaseFirestore.getInstance()
    private val allStudents     = mutableListOf<Map<String, Any>>()
    private val filteredStudents = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_students)

        val rvStudents = findViewById<RecyclerView>(R.id.rvUsers)
        val etSearch   = findViewById<EditText>(R.id.etSearchUser)
        rvStudents.layoutManager = LinearLayoutManager(this)

        adapter = UserAdapter(filteredStudents) { user -> showStudentOptions(user) }
        rvStudents.adapter = adapter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { applySearch(s.toString()) }
        })

        loadStudents()
    }

    private fun loadStudents() {
        db.collection("users").whereEqualTo("role", "student").get()
            .addOnSuccessListener { snap ->
                allStudents.clear()
                for (doc in snap) allStudents.add(doc.data + mapOf("id" to doc.id))
                applySearch("")
            }
            .addOnFailureListener { Toast.makeText(this, "Failed to load students", Toast.LENGTH_SHORT).show() }
    }

    private fun applySearch(query: String) {
        filteredStudents.clear()
        val q = query.lowercase()
        for (s in allStudents) {
            val name  = s["name"]?.toString()?.lowercase() ?: ""
            val email = s["email"]?.toString()?.lowercase() ?: ""
            val reg   = s["registerNo"]?.toString()?.lowercase() ?: ""
            if (name.contains(q) || email.contains(q) || reg.contains(q)) filteredStudents.add(s)
        }
        adapter.notifyDataSetChanged()
    }

    private fun showStudentOptions(student: Map<String, Any>) {
        val blocked = student["blocked"] as? Boolean ?: false
        val options = arrayOf(
            "✏️ Edit Profile",
            if (blocked) "🔓 Unblock Student" else "🔒 Block Student",
            "❌ Cancel"
        )
        AlertDialog.Builder(this)
            .setTitle("👤 Student Options")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> openEditStudent(student)
                    1 -> toggleBlock(student, !blocked)
                }
                dialog.dismiss()
            }.show()
    }

    private fun openEditStudent(student: Map<String, Any>) {
        startActivity(Intent(this, StaffAddStudentActivity::class.java).apply {
            putExtra("mode",       "edit")
            putExtra("studentUid", student["id"] as String)
        })
    }

    private fun toggleBlock(student: Map<String, Any>, block: Boolean) {
        db.collection("users").document(student["id"] as String)
            .update("blocked", block)
            .addOnSuccessListener {
                val msg = if (block) "🔒 Student blocked" else "🔓 Student unblocked"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                loadStudents()
            }
            .addOnFailureListener { Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show() }
    }

    override fun onResume() { super.onResume(); loadStudents() }
}
