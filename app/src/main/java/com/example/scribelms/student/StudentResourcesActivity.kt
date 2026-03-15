package com.example.scribelms.student

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.ResourceAdapter
import com.google.firebase.firestore.FirebaseFirestore

class StudentResourcesActivity : AppCompatActivity() {

    private val db   = FirebaseFirestore.getInstance()
    private val list = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: ResourceAdapter
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_resources)

        val rv  = findViewById<RecyclerView>(R.id.rvResources)
        tvEmpty = findViewById(R.id.tvEmpty)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = ResourceAdapter(list, false, { url -> openUrl(url) })
        rv.adapter = adapter
        load()
    }

    override fun onResume() { super.onResume(); load() }

    private fun load() {
        db.collection("resources").whereEqualTo("active", true).get()
            .addOnSuccessListener { snap ->
                list.clear()
                for (d in snap) list.add(d.data + mapOf("id" to d.id))
                adapter.notifyDataSetChanged()
                tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun openUrl(raw: String) {
        val url = if (raw.startsWith("http")) raw else "https://$raw"
        try {
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE),
                "Open with"))
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Cannot open this link", Toast.LENGTH_LONG).show()
        }
    }
}
