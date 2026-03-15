package com.example.scribelms.student

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.AnnouncementAdapter
import com.google.firebase.firestore.FirebaseFirestore

class StudentAnnouncementsActivity : AppCompatActivity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        setContentView(R.layout.activity_student_announcements)

        val list = mutableListOf<Map<String, Any>>()
        val rv   = findViewById<RecyclerView>(R.id.rvAnnouncements)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = AnnouncementAdapter(list, false) {}

        FirebaseFirestore.getInstance()
            .collection("announcements")
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener {
                list.clear()
                for (d in it) list.add(d.data)
                rv.adapter?.notifyDataSetChanged()
            }
    }
}
