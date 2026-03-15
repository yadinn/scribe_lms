package com.example.scribelms.staff
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.ResourceAdapter
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class StaffResourcesActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val list = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_resources)

        val rv = findViewById<RecyclerView>(R.id.rvResources)
        rv.layoutManager = LinearLayoutManager(this)

        rv.adapter = ResourceAdapter(
            list = list,
            isStaff = true,
            onOpen = { url ->
                openAnyUrl(url)
            },
            onDelete = { id ->
                delete(id)
            }
        )

        findViewById<View>(R.id.fabAdd).setOnClickListener {
            showAddDialog()
        }

        load()
    }
    private fun openAnyUrl(rawUrl: String) {
        val url =
            if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://"))
                rawUrl
            else
                "https://$rawUrl"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addCategory(Intent.CATEGORY_BROWSABLE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Cannot open this link", Toast.LENGTH_LONG).show()
        }
    }

    private fun load() {
        db.collection("resources")
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { snap ->
                list.clear()
                for (d in snap) {
                    list.add(d.data + mapOf("id" to d.id))
                }
                findViewById<RecyclerView>(R.id.rvResources)
                    .adapter?.notifyDataSetChanged()
            }
    }

    private fun showAddDialog() {
        val v = layoutInflater.inflate(R.layout.dialog_add_resource, null)

        val etTitle = v.findViewById<EditText>(R.id.etTitle)
        val etDesc = v.findViewById<EditText>(R.id.etDescription)
        val etUrl = v.findViewById<EditText>(R.id.etUrl)

        AlertDialog.Builder(this)
            .setTitle("Add Resource")
            .setView(v)
            .setPositiveButton("Save") { _, _ ->

                if (etTitle.text.isNullOrBlank() || etUrl.text.isNullOrBlank()) {
                    Toast.makeText(this, "⚠️ Title & URL required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val data = mapOf(
                    "title" to etTitle.text.toString().trim(),
                    "description" to etDesc.text.toString().trim(),
                    "url" to etUrl.text.toString().trim(),
                    "active" to true,
                    "createdAt" to Date()
                )

                db.collection("resources")
                    .add(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, "📎 Resource added", Toast.LENGTH_SHORT).show()
                        load()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun delete(id: String) {
        db.collection("resources")
            .document(id)
            .update("active", false)
            .addOnSuccessListener {
                Toast.makeText(this, "🗑️ Deleted", Toast.LENGTH_SHORT).show()
                load()
            }
    }
}