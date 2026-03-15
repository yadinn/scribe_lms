package com.example.scribelms.student

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.StudentBookAdapter
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class StudentDashboardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val allBooks = mutableListOf<Map<String, Any>>()
    private val filtered  = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: StudentBookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_dashboard)

        val rv       = findViewById<RecyclerView>(R.id.rvBooks)
        val etSearch = findViewById<TextInputEditText>(R.id.etSearch)

        rv.layoutManager = GridLayoutManager(this, 2)
        adapter = StudentBookAdapter(filtered)
        rv.adapter = adapter

        loadBooks()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
        })
    }

    override fun onResume() { super.onResume(); loadBooks() }

    private fun loadBooks() {
        db.collection("books").get().addOnSuccessListener { snap ->
            allBooks.clear()
            for (doc in snap) {
                val map = doc.data.toMutableMap()
                map["id"] = doc.id
                allBooks.add(map)
            }
            filter(findViewById<TextInputEditText>(R.id.etSearch).text.toString())
        }
    }

    private fun filter(q: String) {
        val query = q.trim().lowercase()
        filtered.clear()
        for (b in allBooks) {
            val title    = b["title"]?.toString()?.lowercase()         ?: ""
            val author   = b["author"]?.toString()?.lowercase()        ?: ""
            val category = b["category"]?.toString()?.lowercase()      ?: ""
            val shelf    = b["shelfLocation"]?.toString()?.lowercase() ?: ""
            if (query.isEmpty() || title.contains(query) || author.contains(query)
                || category.contains(query) || shelf.contains(query))
                filtered.add(b)
        }
        adapter.notifyDataSetChanged()
    }
}
