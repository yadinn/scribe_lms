package com.example.scribelms.student

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.scribelms.R
import com.google.firebase.firestore.FirebaseFirestore

class StudentEBookDetailsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_ebook_details)

        val ebookId = intent.getStringExtra("ebookId")
        if (ebookId == null) {
            Toast.makeText(this, "Invalid e-book", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val ivCover = findViewById<ImageView>(R.id.ivCover)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvAuthor = findViewById<TextView>(R.id.tvAuthor)
        val tvDesc = findViewById<TextView>(R.id.tvDesc)
        val btnDownload = findViewById<Button>(R.id.btnDownload)

        db.collection("ebooks").document(ebookId)
            .get()
            .addOnSuccessListener { doc ->

                tvTitle.text = doc.getString("title") ?: "-"
                tvAuthor.text = doc.getString("author") ?: "-"
                tvDesc.text = doc.getString("description") ?: "-"

                Glide.with(this)
                    .load(doc.getString("imageUrl"))
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .into(ivCover)

                btnDownload.setOnClickListener {
                    val url = doc.getString("fileUrl")
                    if (url.isNullOrEmpty()) {
                        Toast.makeText(this, "File not available", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load e-book", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}