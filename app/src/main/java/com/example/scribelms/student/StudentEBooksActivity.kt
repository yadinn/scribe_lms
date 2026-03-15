package com.example.scribelms.student

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.StudentEBooksAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class StudentEBooksActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val all      = mutableListOf<Map<String, Any>>()
    private val filtered = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: StudentEBooksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_ebooks)

        val rv = findViewById<RecyclerView>(R.id.rvEbooks)
        rv.layoutManager = GridLayoutManager(this, 2)

        adapter = StudentEBooksAdapter(filtered) { ebook ->
            startActivity(Intent(this, StudentEBookDetailsActivity::class.java)
                .putExtra("ebookId", ebook["id"].toString()))
        }
        rv.adapter = adapter

        val etSearch = findViewById<TextInputEditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { filter(s.toString()) }
        })

        loadEBooks()
    }

    private fun loadEBooks() {
        db.collection("ebooks").whereEqualTo("active", true).get()
            .addOnSuccessListener { snap ->
                all.clear()
                for (doc in snap) { val m = doc.data.toMutableMap(); m["id"] = doc.id; all.add(m) }
                filter("")
            }
            .addOnFailureListener { Toast.makeText(this, "Failed to load e-books", Toast.LENGTH_SHORT).show() }
    }

    private fun filter(q: String) {
        val query = q.trim().lowercase()
        filtered.clear()
        for (b in all) {
            val t = b["title"]?.toString()?.lowercase()    ?: ""
            val a = b["author"]?.toString()?.lowercase()   ?: ""
            val c = b["category"]?.toString()?.lowercase() ?: ""
            if (query.isEmpty() || t.contains(query) || a.contains(query) || c.contains(query))
                filtered.add(b)
        }
        adapter.notifyDataSetChanged()
    }
}
