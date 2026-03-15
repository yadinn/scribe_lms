package com.example.scribelms.staff

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.StaffEBooksAdapter
import com.google.firebase.firestore.FirebaseFirestore

class StaffManageEBooksActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val list = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: StaffEBooksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_manage_ebooks)

        // ➕ ADD EBOOK BUTTON (FIX)
        findViewById<Button>(R.id.btnAddEBook).setOnClickListener {
            startActivity(
                Intent(this, StaffAddEBookActivity::class.java)
            )
        }

        val rv = findViewById<RecyclerView>(R.id.rvEBooks)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = StaffEBooksAdapter(
            list = list,
            onEdit = { ebook ->
                val intent = Intent(this, StaffAddEBookActivity::class.java)
                intent.putExtra("ebookId", ebook["id"].toString())
                intent.putExtra("title", ebook["title"]?.toString() ?: "")
                intent.putExtra("author", ebook["author"]?.toString() ?: "")
                intent.putExtra("category", ebook["category"]?.toString() ?: "")
                intent.putExtra("description", ebook["description"]?.toString() ?: "")
                intent.putExtra("imageUrl", ebook["imageUrl"]?.toString() ?: "")
                intent.putExtra("fileUrl", ebook["fileUrl"]?.toString() ?: "")
                startActivity(intent)
            },
            onDelete = { ebookId ->
                confirmDelete(ebookId)
            }
        )

        rv.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        loadEBooks()
    }

    private fun loadEBooks() {
        db.collection("ebooks")
            .get()
            .addOnSuccessListener { snap ->
                list.clear()
                for (doc in snap) {
                    val map = doc.data.toMutableMap()
                    map["id"] = doc.id
                    list.add(map)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, "❌ Failed to load e-books", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmDelete(id: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete E-Book")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("ebooks").document(id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "🗑️ Deleted", Toast.LENGTH_SHORT).show()
                        loadEBooks()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}