package com.example.scribelms.staff

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.ManageBookAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class ManageBooksActivity : AppCompatActivity() {

    private val db       = FirebaseFirestore.getInstance()
    private val bookList = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: ManageBookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_books)

        val rv    = findViewById<RecyclerView>(R.id.rvBooks)
        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAddBook)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = ManageBookAdapter(bookList) { book -> showOptions(book) }
        rv.adapter = adapter

        fabAdd.setOnClickListener { startActivity(Intent(this, AddBookActivity::class.java)) }
        loadBooks()
    }

    override fun onResume() { super.onResume(); loadBooks() }

    private fun loadBooks() {
        db.collection("books").get().addOnSuccessListener { snap ->
            bookList.clear()
            for (doc in snap) { val d = doc.data.toMutableMap(); d["bookId"] = doc.id; bookList.add(d) }
            adapter.notifyDataSetChanged()
        }
    }

    private fun showOptions(book: Map<String, Any>) {
        AlertDialog.Builder(this)
            .setTitle("📚 ${book["title"]?.toString() ?: "Book Options"}")
            .setItems(arrayOf("✏️ Edit", "📜 History", "🗑️ Delete")) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(this, AddBookActivity::class.java).putExtra("bookId", book["bookId"] as String))
                    1 -> startActivity(Intent(this, StaffBookHistoryActivity::class.java).putExtra("bookId", book["bookId"] as String))
                    2 -> confirmDelete(book)
                }
            }.show()
    }

    private fun confirmDelete(book: Map<String, Any>) {
        val issued = ((book["totalCopies"] as? Long ?: 0) - (book["availableCopies"] as? Long ?: 0))
        if (issued > 0) {
            AlertDialog.Builder(this)
                .setTitle("⚠️ Cannot Delete")
                .setMessage("This book has $issued copy/copies currently issued. Return all copies before deleting.")
                .setPositiveButton("OK", null).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("🗑️ Delete Book")
            .setMessage("Delete \"${book["title"]}\"? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                db.collection("books").document(book["bookId"] as String).delete()
                    .addOnSuccessListener { loadBooks() }
            }
            .setNegativeButton("Cancel", null).show()
    }
}
