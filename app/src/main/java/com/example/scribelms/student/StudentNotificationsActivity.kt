package com.example.scribelms.student

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.NotificationAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StudentNotificationsActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_notifications)

        val rv = findViewById<RecyclerView>(R.id.rvNotifications)
        rv.layoutManager = LinearLayoutManager(this)

        loadNotifications(rv)
    }

    private fun loadNotifications(rv: RecyclerView) {

        val uid = auth.currentUser?.uid ?: return

        db.collection("notifications")
            .whereIn("userUid", listOf(uid, "ALL"))
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { snap ->

                val list = snap.map { it.id to it.data }

                rv.adapter = NotificationAdapter(list) { notifId ->
                    db.collection("notifications")
                        .document(notifId)
                        .update("read", true)
                }
            }
    }
}