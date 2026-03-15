package com.example.scribelms.staff

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.scribelms.R
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class StaffAddEBookActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var editId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_add_ebook)

        val etTitle       = findViewById<EditText>(R.id.etTitle)
        val etAuthor      = findViewById<EditText>(R.id.etAuthor)
        val etCategory    = findViewById<EditText>(R.id.etCategory)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etImageUrl    = findViewById<EditText>(R.id.etImageUrl)
        val etFileUrl     = findViewById<EditText>(R.id.etPdfUrl)
        val ivPreview     = findViewById<ImageView>(R.id.ivPreview)
        val btnSave       = findViewById<Button>(R.id.btnSaveEBook)

        editId = intent.getStringExtra("ebookId")
        if (editId != null) {
            etTitle.setText(intent.getStringExtra("title") ?: "")
            etAuthor.setText(intent.getStringExtra("author") ?: "")
            etCategory.setText(intent.getStringExtra("category") ?: "")
            etDescription.setText(intent.getStringExtra("description") ?: "")
            etImageUrl.setText(intent.getStringExtra("imageUrl") ?: "")
            etFileUrl.setText(intent.getStringExtra("fileUrl") ?: "")
            btnSave.text = "✅ Update E-Book"
            val imgUrl = intent.getStringExtra("imageUrl") ?: ""
            if (imgUrl.isNotEmpty()) Glide.with(this).load(imgUrl)
                .placeholder(R.drawable.ic_book_placeholder).error(R.drawable.ic_book_placeholder).into(ivPreview)
        } else {
            btnSave.text = "📱 Add E-Book"
        }

        etImageUrl.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val url = s.toString().trim()
                if (url.isNotEmpty()) Glide.with(this@StaffAddEBookActivity).load(url)
                    .placeholder(R.drawable.ic_book_placeholder).error(R.drawable.ic_book_placeholder).into(ivPreview)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnSave.setOnClickListener {
            val title       = etTitle.text.toString().trim()
            val author      = etAuthor.text.toString().trim()
            val category    = etCategory.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val imageUrl    = etImageUrl.text.toString().trim()
            val fileUrl     = etFileUrl.text.toString().trim()

            if (title.isEmpty() || fileUrl.isEmpty()) {
                Toast.makeText(this, "⚠️ Title & PDF URL required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = hashMapOf<String, Any>(
                "title" to title, "author" to author, "category" to category,
                "description" to description, "imageUrl" to imageUrl,
                "fileUrl" to fileUrl, "active" to true
            )

            if (editId != null) {
                db.collection("ebooks").document(editId!!).update(data)
                    .addOnSuccessListener { Toast.makeText(this, "✅ E-Book updated", Toast.LENGTH_SHORT).show(); finish() }
                    .addOnFailureListener { Toast.makeText(this, "❌ Failed: ${it.message}", Toast.LENGTH_LONG).show() }
            } else {
                data["createdAt"] = Date()
                db.collection("ebooks").add(data)
                    .addOnSuccessListener { Toast.makeText(this, "📱 E-Book added!", Toast.LENGTH_SHORT).show(); finish() }
                    .addOnFailureListener { Toast.makeText(this, "❌ Failed: ${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
    }
}
