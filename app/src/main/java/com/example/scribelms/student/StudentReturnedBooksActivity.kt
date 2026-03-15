package com.example.scribelms.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.StudentReturnedBooksAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentReturnedBooksActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val list = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: StudentReturnedBooksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_returned_books)

        val rv = findViewById<RecyclerView>(R.id.rvReturnedBooks)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        rv.layoutManager = LinearLayoutManager(this)

        adapter = StudentReturnedBooksAdapter(list) { bookId ->
            val i = Intent(this, StudentFeedbackActivity::class.java)
            i.putExtra("bookId", bookId)
            startActivity(i)
        }

        rv.adapter = adapter

        tvEmpty.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadReturnedBooks()
    }

    private fun loadReturnedBooks() {

        val uid = auth.currentUser?.uid ?: return

        list.clear()
        adapter.notifyDataSetChanged()

        db.collection("issue_requests")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("status", "returned")
            .get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) {
                    findViewById<TextView>(R.id.tvEmpty).visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                findViewById<TextView>(R.id.tvEmpty).visibility = View.GONE

                for (doc in snap) {

                    val bookId = doc.getString("bookId") ?: continue

                    db.collection("books").document(bookId)
                        .get()
                        .addOnSuccessListener { bookDoc ->

                            val map = hashMapOf<String, Any>(
                                "bookId" to bookId,
                                "title" to (bookDoc.getString("title") ?: "-")
                            )

                            // check if review already exists
                            db.collection("reviews")
                                .whereEqualTo("studentUid", uid)
                                .whereEqualTo("bookId", bookId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener { reviewSnap ->

                                    map["hasReview"] = !reviewSnap.isEmpty
                                    list.add(map)
                                    adapter.notifyDataSetChanged()
                                }
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load returned books", Toast.LENGTH_SHORT).show()
            }
    }
}