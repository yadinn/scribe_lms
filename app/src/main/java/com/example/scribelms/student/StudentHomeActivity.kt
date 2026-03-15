package com.example.scribelms.student

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.scribelms.R
import com.example.scribelms.auth.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class StudentHomeActivity : AppCompatActivity() {

    private val db            = FirebaseFirestore.getInstance()
    private val auth          = FirebaseAuth.getInstance()
    private val PREFS         = "scribe_prefs"
    private val KEY_LAST_SEEN = "last_seen_announcement"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_home)

        fun go(id: Int, cls: Class<*>) =
            findViewById<LinearLayout>(id).setOnClickListener { startActivity(Intent(this, cls)) }

        go(R.id.btnBooks,          StudentDashboardActivity::class.java)
        go(R.id.btnIssuedBooks,    StudentIssuedBooksActivity::class.java)
        go(R.id.btnCollectionCard, StudentCollectionCardActivity::class.java)
        go(R.id.btnEditProfile,    StudentEditProfileActivity::class.java)
        go(R.id.btnRequestStatus,  StudentRequestStatusActivity::class.java)
        go(R.id.btnEBooks,         StudentEBooksActivity::class.java)
        go(R.id.btnResources,      StudentResourcesActivity::class.java)
        go(R.id.btnMyReviews,      StudentReturnedBooksActivity::class.java)
        go(R.id.btnBookHistory,    StudentBookHistoryActivity::class.java)
        go(R.id.btnFineHistory,    StudentFineHistoryActivity::class.java)

        // Announcements — clear badge on open
        findViewById<LinearLayout>(R.id.btnAnnouncements).setOnClickListener {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putLong(KEY_LAST_SEEN, System.currentTimeMillis()).apply()
            clearAnnouncementBadge()
            startActivity(Intent(this, StudentAnnouncementsActivity::class.java))
        }

        // Helpdesk — clear unread badge on open
        findViewById<LinearLayout>(R.id.btnHelpdesk).setOnClickListener {
            clearHelpdeskBadge()
            startActivity(Intent(this, StudentHelpdeskActivity::class.java))
        }

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .setNegativeButton("Cancel", null).show()
        }

        // Load student name
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name")?.substringBefore(" ") ?: ""
                if (name.isNotEmpty()) {
                    findViewById<TextView>(R.id.tvWelcomeName).text = "👋 Hi, $name!"
                }
            }
    }

    override fun onResume() {
        super.onResume()
        loadAnnouncementBadge()
        loadHelpdeskBadge()
        loadStats()
    }

    private fun loadStats() {
        val uid = auth.currentUser?.uid ?: return
        // Count issued books
        db.collection("issue_requests")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("status", "issued")
            .get()
            .addOnSuccessListener { snap ->
                findViewById<TextView>(R.id.tvStatIssued).text = snap.size().toString()
            }
        // Count pending requests
        db.collection("issue_requests")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snap ->
                findViewById<TextView>(R.id.tvStatPending).text = snap.size().toString()
            }
        // Count unpaid fines
        db.collection("fines")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("paid", false)
            .get()
            .addOnSuccessListener { snap ->
                val total = snap.documents.sumOf { (it.getLong("amount") ?: 0L) }
                findViewById<TextView>(R.id.tvStatFines).text =
                    if (total > 0) "₹$total" else "0"
            }
    }

    private fun loadAnnouncementBadge() {
        val lastSeen = getSharedPreferences(PREFS, MODE_PRIVATE).getLong(KEY_LAST_SEEN, 0L)
        db.collection("announcements")
            .whereGreaterThan("createdAt", Timestamp(Date(lastSeen)))
            .get()
            .addOnSuccessListener { snap ->
                val badge = findViewById<TextView>(R.id.tvAnnouncementBadge)
                val count = snap.size()
                badge.visibility = if (count > 0) View.VISIBLE else View.GONE
                badge.text       = if (count > 9) "9+" else count.toString()
            }
    }

    private fun clearAnnouncementBadge() {
        findViewById<TextView>(R.id.tvAnnouncementBadge).visibility = View.GONE
    }

    private fun loadHelpdeskBadge() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("helpdesk_threads").document("student_$uid").get()
            .addOnSuccessListener { doc ->
                val badge  = findViewById<TextView?>(R.id.tvHelpdeskBadge) ?: return@addOnSuccessListener
                val unread = doc.getBoolean("unreadStudent") ?: false
                badge.visibility = if (unread) View.VISIBLE else View.GONE
            }
    }

    private fun clearHelpdeskBadge() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("helpdesk_threads").document("student_$uid")
            .update("unreadStudent", false)
        findViewById<TextView?>(R.id.tvHelpdeskBadge)?.visibility = View.GONE
    }
}
