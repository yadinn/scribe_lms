package com.example.scribelms.admin

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.scribelms.R
import com.example.scribelms.auth.LoginActivity
import com.example.scribelms.staff.StaffAddStudentActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminHomeActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_home)

        fun nav(id: Int, cls: Class<*>) =
            findViewById<LinearLayout>(id)?.setOnClickListener { startActivity(Intent(this, cls)) }

        nav(R.id.btnAddStaff,    AdminAddStaffActivity::class.java)
        nav(R.id.btnAddStudent,  StaffAddStudentActivity::class.java)
        nav(R.id.btnViewUsers,   ViewUsersActivity::class.java)
        nav(R.id.btnSettings,    AdminSettingsActivity::class.java)

        findViewById<LinearLayout>(R.id.btnSoftReset).setOnClickListener { confirmSoftReset() }

        findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("🚪 Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                }
                .setNegativeButton("Cancel", null).show()
        }
    }

    private fun confirmSoftReset() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Soft Reset Library")
            .setMessage("This will:\n\n• 🗑️ Delete all issue & return records\n• 📜 Delete all history\n• 📚 Restore all book copies to full\n• 👤 Keep all users intact\n\nThis action CANNOT be undone. Continue?")
            .setPositiveButton("RESET") { _, _ -> performSoftReset() }
            .setNegativeButton("Cancel", null).show()
    }

    private fun performSoftReset() {
        val booksRef  = db.collection("books")
        val issueRef  = db.collection("issue_requests")

        booksRef.get().addOnSuccessListener { booksSnap ->
            issueRef.get().addOnSuccessListener { reqSnap ->
                val allDocs = reqSnap.documents
                val chunks  = allDocs.chunked(400)

                fun processNextChunk(index: Int) {
                    if (index >= chunks.size) {
                        val bookBatch = db.batch()
                        for (d in booksSnap.documents) {
                            bookBatch.update(d.reference, "availableCopies", d.getLong("totalCopies") ?: 0)
                        }
                        bookBatch.commit()
                            .addOnSuccessListener { Toast.makeText(this, "✅ Soft reset completed", Toast.LENGTH_LONG).show() }
                            .addOnFailureListener { Toast.makeText(this, "❌ Books restore failed: ${it.message}", Toast.LENGTH_LONG).show() }
                        return
                    }
                    val batch = db.batch()
                    for (d in chunks[index]) batch.delete(d.reference)
                    batch.commit()
                        .addOnSuccessListener { processNextChunk(index + 1) }
                        .addOnFailureListener { Toast.makeText(this, "❌ Reset failed: ${it.message}", Toast.LENGTH_LONG).show() }
                }

                if (allDocs.isEmpty()) {
                    val bookBatch = db.batch()
                    for (d in booksSnap.documents) {
                        bookBatch.update(d.reference, "availableCopies", d.getLong("totalCopies") ?: 0)
                    }
                    bookBatch.commit().addOnSuccessListener { Toast.makeText(this, "✅ Library reset complete", Toast.LENGTH_LONG).show() }
                } else processNextChunk(0)
            }
        }
    }
}
