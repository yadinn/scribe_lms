package com.example.scribelms.staff

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.StaffStudentReviewsAdapter
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class StaffStudentReviewsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val all = mutableListOf<Map<String, Any>>()
    private val filtered = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: StaffStudentReviewsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_student_reviews)

        val rv = findViewById<RecyclerView>(R.id.rvReviews)
        val etStudent = findViewById<EditText>(R.id.etSearchStudent)
        val etBook = findViewById<EditText>(R.id.etSearchBook)

        rv.layoutManager = LinearLayoutManager(this)

        adapter = StaffStudentReviewsAdapter(filtered) {
            loadReviews()
        }
        rv.adapter = adapter

        loadReviews()

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(etStudent.text.toString(), etBook.text.toString())
            }
        }

        etStudent.addTextChangedListener(watcher)
        etBook.addTextChangedListener(watcher)
    }

    private fun loadReviews() {

        all.clear()
        filtered.clear()
        adapter.notifyDataSetChanged()

        db.collection("reviews")
            .get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) return@addOnSuccessListener

                for (doc in snap) {

                    val studentUid = doc.getString("studentUid") ?: continue
                    val bookId = doc.getString("bookId") ?: continue

                    db.collection("users").document(studentUid).get()
                        .addOnSuccessListener { studentDoc ->

                            val studentName = studentDoc.getString("name") ?: "Unknown"

                            db.collection("books").document(bookId).get()
                                .addOnSuccessListener { bookDoc ->

                                    val map = hashMapOf<String, Any>(
                                        "docId" to doc.id,
                                        "studentName" to studentName,
                                        "bookTitle" to (bookDoc.getString("title") ?: "-"),
                                        "rating" to (doc.getLong("rating") ?: 0),
                                        "comment" to (doc.getString("comment") ?: ""),
                                        "updatedAt" to (doc.getDate("updatedAt") ?: Date())
                                    )

                                    all.add(map)
                                    filter("", "")
                                }
                        }
                }
            }
    }

    private fun filter(student: String, book: String) {

        val s = student.lowercase()
        val b = book.lowercase()

        filtered.clear()

        for (r in all) {
            val sn = r["studentName"].toString().lowercase()
            val bn = r["bookTitle"].toString().lowercase()

            if (sn.contains(s) && bn.contains(b)) {
                filtered.add(r)
            }
        }

        adapter.notifyDataSetChanged()
    }
}