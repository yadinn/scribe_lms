package com.example.scribelms.student

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.scribelms.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class StudentBookDetailsActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var bookId: String
    private var maxLimit   = 3
    private var takenCount = 0
    private var bookTitle  = "-"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_book_details)

        bookId = intent.getStringExtra("bookId") ?: run {
            Toast.makeText(this, "Invalid book", Toast.LENGTH_SHORT).show(); finish(); return
        }

        val ivImage     = findViewById<ImageView>(R.id.ivBookImage)
        val tvTitle     = findViewById<TextView>(R.id.tvTitle)
        val tvAuthor    = findViewById<TextView>(R.id.tvAuthor)
        val tvDesc      = findViewById<TextView>(R.id.tvDescription)
        val tvCopies    = findViewById<TextView>(R.id.tvCopies)
        val tvShelf     = findViewById<TextView>(R.id.tvShelfLocation)
        val tvLimit     = findViewById<TextView>(R.id.tvLimit)
        val btnIssue    = findViewById<Button>(R.id.btnIssueBook)
        val rvReviews   = findViewById<RecyclerView>(R.id.rvReviews)
        val tvNoReviews = findViewById<TextView>(R.id.tvNoReviews)

        rvReviews.layoutManager = LinearLayoutManager(this)

        loadBook(ivImage, tvTitle, tvAuthor, tvDesc, tvCopies, tvShelf, btnIssue)
        loadSettings { loadTakenCount { updateLimitUI(tvLimit, btnIssue) } }
        loadReviews(rvReviews, tvNoReviews)

        btnIssue.setOnClickListener { checkUnpaidFineThenIssue(btnIssue) }
    }

    private fun loadSettings(done: () -> Unit) {
        db.collection("settings").document("library").get()
            .addOnSuccessListener { maxLimit = it.getLong("maxIssueLimit")?.toInt() ?: 3; done() }
            .addOnFailureListener { maxLimit = 3; done() }
    }

    private fun loadTakenCount(done: () -> Unit) {
        db.collection("issue_requests")
            .whereEqualTo("studentUid", auth.uid)
            .whereIn("status", listOf("pending", "approved", "issued"))
            .get()
            .addOnSuccessListener { takenCount = it.size(); done() }
    }

    private fun updateLimitUI(tvLimit: TextView, btnIssue: Button) {
        tvLimit.text = "Books taken: $takenCount / $maxLimit"
        if (takenCount >= maxLimit) {
            btnIssue.isEnabled = false
            tvLimit.setTextColor(getColor(android.R.color.holo_red_dark))
        }
    }

    private fun loadBook(
        iv: ImageView, tvTitle: TextView, tvAuthor: TextView,
        tvDesc: TextView, tvCopies: TextView, tvShelf: TextView, btnIssue: Button
    ) {
        db.collection("books").document(bookId).get()
            .addOnSuccessListener { doc ->
                bookTitle     = doc.getString("title") ?: "-"
                tvTitle.text  = bookTitle
                tvAuthor.text = "Author: ${doc.getString("author") ?: "-"}"
                tvDesc.text   = doc.getString("description") ?: "-"

                val avail = doc.getLong("availableCopies") ?: 0
                val total = doc.getLong("totalCopies") ?: avail
                tvCopies.text = "Available: $avail / $total"
                tvCopies.setTextColor(
                    if (avail <= 0) getColor(android.R.color.holo_red_dark)
                    else getColor(android.R.color.holo_green_dark)
                )
                if (avail <= 0) btnIssue.isEnabled = false

                val shelf = doc.getString("shelfLocation")
                if (!shelf.isNullOrBlank()) {
                    tvShelf.visibility = View.VISIBLE
                    tvShelf.text = "📍 $shelf"
                } else tvShelf.visibility = View.GONE

                Glide.with(this).load(doc.getString("imageUrl"))
                    .placeholder(R.drawable.ic_book_placeholder)
                    .error(R.drawable.ic_book_placeholder)
                    .fitCenter().into(iv)
            }
    }

    private fun checkUnpaidFineThenIssue(btn: Button) {
        val uid = auth.uid ?: return
        db.collection("issue_requests")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("status", "returned")
            .get()
            .addOnSuccessListener { snap ->
                val hasUnpaid = snap.any { (it.getLong("fineAmount") ?: 0) > 0 && it.getBoolean("finePaid") != true }
                if (hasUnpaid) {
                    Toast.makeText(this, "You have unpaid fines. Clear them before requesting new books.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                sendIssueRequest(btn)
            }
    }

    private fun sendIssueRequest(btn: Button) {
        val uid = auth.uid ?: return
        btn.isEnabled = false
        db.collection("issue_requests")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("bookId", bookId)
            .whereIn("status", listOf("pending", "approved", "issued"))
            .get()
            .addOnSuccessListener { existing ->
                if (!existing.isEmpty) {
                    Toast.makeText(this, "You already have an active request for this book", Toast.LENGTH_LONG).show()
                    btn.isEnabled = true; return@addOnSuccessListener
                }
                db.collection("issue_requests").add(hashMapOf(
                    "bookId" to bookId, "bookTitle" to bookTitle,
                    "studentUid" to uid, "status" to "pending",
                    "requestedAt" to Date(), "returnRequested" to false
                )).addOnSuccessListener {
                    Toast.makeText(this, "Request sent!", Toast.LENGTH_SHORT).show()
                }.addOnFailureListener {
                    Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                }
            }
    }

    // ── Reviews with star rating + student name ──────────────────────────────
    private fun loadReviews(rv: RecyclerView, tvNoReviews: TextView) {
        db.collection("reviews").whereEqualTo("bookId", bookId).get()
            .addOnSuccessListener { snap ->
                if (snap.isEmpty) {
                    tvNoReviews.visibility = View.VISIBLE
                    rv.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val reviews = snap.documents.map { doc ->
                    mapOf(
                        "rating"      to (doc.getLong("rating") ?: 0L),
                        "comment"     to (doc.getString("comment") ?: ""),
                        "studentUid"  to (doc.getString("studentUid") ?: ""),
                        "studentName" to (doc.getString("studentName") ?: "")
                    )
                }

                rv.adapter = ReviewAdapter(reviews)
                rv.visibility = View.VISIBLE
                tvNoReviews.visibility = View.GONE
            }
    }

    // ── Inline adapter for review cards ──────────────────────────────────────
    inner class ReviewAdapter(
        private val items: List<Map<String, Any>>
    ) : RecyclerView.Adapter<ReviewAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName    = v.findViewById<TextView>(R.id.tvReviewerName)
            val tvStars   = v.findViewById<TextView>(R.id.tvStars)
            val tvComment = v.findViewById<TextView>(R.id.tvReviewComment)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_review_card, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val r      = items[pos]
            val rating = (r["rating"] as? Long)?.toInt() ?: 0
            val name   = r["studentName"]?.toString()?.takeIf { it.isNotBlank() }
                ?: r["studentUid"]?.toString()?.take(8) ?: "Student"

            h.tvName.text    = name
            h.tvStars.text   = "★".repeat(rating) + "☆".repeat((5 - rating).coerceAtLeast(0))
            h.tvStars.setTextColor(Color.parseColor("#FFA000"))
            h.tvComment.text = r["comment"]?.toString() ?: ""
        }
    }
}
