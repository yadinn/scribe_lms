package com.example.scribelms.staff

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.scribelms.R
import com.google.firebase.firestore.FirebaseFirestore

class AddBookActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private var bookId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_book)

        val etTitle    = findViewById<EditText>(R.id.etTitle)
        val etAuthor   = findViewById<EditText>(R.id.etAuthor)
        val etDesc     = findViewById<EditText>(R.id.etDescription)
        val etCopies   = findViewById<EditText>(R.id.etCopies)
        val etShelf    = findViewById<EditText>(R.id.etShelfLocation)
        val etImageUrl = findViewById<EditText>(R.id.etImageUrl)
        val ivImage    = findViewById<ImageView>(R.id.ivBookImage)
        val btnSave    = findViewById<Button>(R.id.btnSaveBook)

        bookId = intent.getStringExtra("bookId")

        if (bookId != null) {
            btnSave.text = "✅ Update Book"
            db.collection("books").document(bookId!!).get()
                .addOnSuccessListener { doc ->
                    etTitle.setText(doc.getString("title"))
                    etAuthor.setText(doc.getString("author"))
                    etDesc.setText(doc.getString("description"))
                    etCopies.setText((doc.getLong("totalCopies") ?: 0).toString())
                    etShelf.setText(doc.getString("shelfLocation") ?: "")
                    etImageUrl.setText(doc.getString("imageUrl"))
                    loadImage(doc.getString("imageUrl"), ivImage)
                }
        } else {
            btnSave.text = "📚 Add Book"
        }

        etImageUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { loadImage(s.toString(), ivImage) }
        })

        btnSave.setOnClickListener {
            val title    = etTitle.text.toString().trim()
            val author   = etAuthor.text.toString().trim()
            val desc     = etDesc.text.toString().trim()
            val shelf    = etShelf.text.toString().trim()
            val newTotal = etCopies.text.toString().toIntOrNull() ?: 0
            val imageUrl = fixImageUrl(etImageUrl.text.toString().trim())

            if (title.isEmpty()) { etTitle.error = "Required"; return@setOnClickListener }

            if (bookId == null) {
                db.collection("books").add(mapOf(
                    "title" to title, "author" to author, "description" to desc,
                    "totalCopies" to newTotal, "availableCopies" to newTotal,
                    "shelfLocation" to shelf, "imageUrl" to imageUrl
                )).addOnSuccessListener { Toast.makeText(this, "📚 Book added!", Toast.LENGTH_SHORT).show(); finish() }
                  .addOnFailureListener { Toast.makeText(this, "❌ Failed: ${it.message}", Toast.LENGTH_SHORT).show() }
            } else {
                db.collection("books").document(bookId!!).get()
                    .addOnSuccessListener { snap ->
                        val oldTotal = (snap.getLong("totalCopies")     ?: newTotal.toLong()).toInt()
                        val oldAvail = (snap.getLong("availableCopies") ?: 0).toInt()
                        val newAvail = (oldAvail + (newTotal - oldTotal)).coerceAtLeast(0)
                        db.collection("books").document(bookId!!).update(mapOf(
                            "title" to title, "author" to author, "description" to desc,
                            "totalCopies" to newTotal, "availableCopies" to newAvail,
                            "shelfLocation" to shelf, "imageUrl" to imageUrl
                        )).addOnSuccessListener { Toast.makeText(this, "✅ Book updated", Toast.LENGTH_SHORT).show(); finish() }
                          .addOnFailureListener { Toast.makeText(this, "❌ Failed: ${it.message}", Toast.LENGTH_SHORT).show() }
                    }
            }
        }
    }

    private fun fixImageUrl(url: String): String {
        if (url.contains("github.com") && url.contains("/blob/"))
            return url.replace("github.com", "raw.githubusercontent.com").replace("/blob/", "/")
        return url
    }

    private fun loadImage(url: String?, iv: ImageView) {
        val fixed = fixImageUrl(url ?: "")
        if (fixed.isNotEmpty()) Glide.with(this).load(fixed)
            .placeholder(R.drawable.ic_book_placeholder).error(R.drawable.ic_book_placeholder).into(iv)
    }
}
