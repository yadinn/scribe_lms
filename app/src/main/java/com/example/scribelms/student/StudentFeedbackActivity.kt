package com.example.scribelms.student

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.scribelms.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class StudentFeedbackActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var bookId: String
    private var reviewDocId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_feedback)

        bookId = intent.getStringExtra("bookId") ?: run { finish(); return }

        val tvBookName = findViewById<TextView>(R.id.tvBookName)
        val tvAuthor   = findViewById<TextView>(R.id.tvAuthor)
        val ratingBar  = findViewById<RatingBar>(R.id.ratingBar)
        val etReview   = findViewById<EditText>(R.id.etReview)
        val btnSave    = findViewById<Button>(R.id.btnSave)
        val btnDelete  = findViewById<Button>(R.id.btnDelete)
        val uid        = auth.currentUser?.uid ?: run { finish(); return }

        btnDelete.visibility = Button.GONE

        db.collection("books").document(bookId).get()
            .addOnSuccessListener { doc ->
                tvBookName.text = "📚 ${doc.getString("title") ?: "-"}"
                tvAuthor.text   = "✍️ ${doc.getString("author") ?: "-"}"
            }

        db.collection("reviews")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("bookId", bookId)
            .limit(1).get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    val doc = snap.documents[0]
                    reviewDocId = doc.id
                    ratingBar.rating = (doc.getLong("rating") ?: 0).toFloat()
                    etReview.setText(doc.getString("comment") ?: "")
                    btnDelete.visibility = Button.VISIBLE
                }
            }

        btnSave.setOnClickListener {
            val rating  = ratingBar.rating.toInt()
            val comment = etReview.text.toString().trim()
            if (rating == 0) {
                Toast.makeText(this, "⭐ Please give a star rating", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val data = hashMapOf<String, Any>(
                "studentUid" to uid, "bookId" to bookId,
                "rating" to rating, "comment" to comment, "updatedAt" to Date()
            )
            db.collection("users").document(uid).get()
                .addOnSuccessListener { userDoc ->
                    val name = userDoc.getString("name") ?: ""
                    if (name.isNotBlank()) data["studentName"] = name
                    saveReview(data)
                }
                .addOnFailureListener { saveReview(data) }
        }

        btnDelete.setOnClickListener {
            val docId = reviewDocId ?: return@setOnClickListener
            db.collection("reviews").document(docId).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "🗑️ Review deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun saveReview(data: HashMap<String, Any>) {
        val docId = reviewDocId
        if (docId == null) {
            db.collection("reviews").add(data)
                .addOnSuccessListener { Toast.makeText(this, "⭐ Review added!", Toast.LENGTH_SHORT).show(); finish() }
        } else {
            db.collection("reviews").document(docId).update(data as Map<String, Any>)
                .addOnSuccessListener { Toast.makeText(this, "✅ Review updated", Toast.LENGTH_SHORT).show(); finish() }
        }
    }
}
