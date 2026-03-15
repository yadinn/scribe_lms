package com.example.scribelms.staff

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.scribelms.R
import com.example.scribelms.helpdesk.StaffHelpdeskListActivity
import com.example.scribelms.auth.LoginActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class StaffHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_home)

        fun nav(id: Int, cls: Class<*>) =
            findViewById<LinearLayout>(id)?.setOnClickListener { startActivity(Intent(this, cls)) }

        nav(R.id.btnPendingRequests, StaffIssueRequestsActivity::class.java)
        nav(R.id.btnScanQr,          StaffQrScanActivity::class.java)
        nav(R.id.btnIssuedBooks,     StaffIssuedBooksActivity::class.java)
        nav(R.id.btnOverdueBooks,    StaffOverdueBooksActivity::class.java)
        nav(R.id.btnIssueHistory,    StaffHistoryActivity::class.java)
        nav(R.id.btnManageBooks,     ManageBooksActivity::class.java)
        nav(R.id.btnManageEBooks,    StaffManageEBooksActivity::class.java)
        nav(R.id.btnViewStudents,    ViewStudentsActivity::class.java)
        nav(R.id.btnAddStudent,      StaffAddStudentActivity::class.java)
        nav(R.id.btnAnnouncements,   StaffAnnouncementsActivity::class.java)
        nav(R.id.btnStudentReviews,  StaffStudentReviewsActivity::class.java)
        nav(R.id.btnResources,       StaffResourcesActivity::class.java)
        nav(R.id.btnHelpdeskList,    StaffHelpdeskListActivity::class.java)
        nav(R.id.btnFineHistory,     StaffFineHistoryActivity::class.java)

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
}
