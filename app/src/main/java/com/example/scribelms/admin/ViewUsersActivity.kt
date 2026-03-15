package com.example.scribelms.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class ViewUsersActivity : AppCompatActivity() {

    private val db            = FirebaseFirestore.getInstance()
    private val allUsers      = mutableListOf<Map<String, Any>>()
    private val filteredUsers = mutableListOf<Map<String, Any>>()
    private lateinit var adapter: UserAdapter
    private lateinit var spinner: Spinner
    private lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_users)

        val rvUsers = findViewById<RecyclerView>(R.id.rvUsers)
        etSearch    = findViewById(R.id.etSearchUser)
        spinner     = findViewById(R.id.spinnerRole)

        rvUsers.layoutManager = LinearLayoutManager(this)

        adapter = UserAdapter(filteredUsers) { user -> showBlockDialog(user) }
        rvUsers.adapter = adapter

        val roles = listOf("All", "student", "staff", "admin")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters(spinner.selectedItem.toString(), s.toString())
            }
        })

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) {
                applyFilters(spinner.selectedItem.toString(), etSearch.text.toString())
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    private fun loadUsers() {
        db.collection("users").get()
            .addOnSuccessListener { snapshot ->
                allUsers.clear()
                for (doc in snapshot) {
                    val data = doc.data.toMutableMap()
                    data["id"] = doc.id
                    // Ensure 'blocked' field exists in local map
                    if (!data.containsKey("blocked")) data["blocked"] = false
                    allUsers.add(data)
                }
                applyFilters(spinner.selectedItem?.toString() ?: "All", etSearch.text.toString())
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load users: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilters(role: String, query: String) {
        filteredUsers.clear()
        for (user in allUsers) {
            val userRole   = user["role"]?.toString() ?: ""
            val name       = user["name"]?.toString()?.lowercase() ?: ""
            val email      = user["email"]?.toString()?.lowercase() ?: ""
            val roleMatch  = role == "All" || role == userRole
            val queryMatch = query.isBlank() || name.contains(query.lowercase()) || email.contains(query.lowercase())
            if (roleMatch && queryMatch) filteredUsers.add(user)
        }
        adapter.notifyDataSetChanged()
    }

    private fun showBlockDialog(user: Map<String, Any>) {
        val uid     = user["id"]?.toString() ?: return
        val name    = user["name"]?.toString() ?: "User"
        val blocked = user["blocked"] as? Boolean ?: false
        val action  = if (blocked) "Unblock" else "Block"
        val icon    = if (blocked) "🔓" else "🔒"
        val msg     = if (blocked)
            "Unblock $name? They will be able to log in again."
        else
            "Block $name? They will be signed out and cannot log in until unblocked."

        AlertDialog.Builder(this)
            .setTitle("$icon $action User")
            .setMessage(msg)
            .setPositiveButton(action) { _, _ ->
                blockOrUnblock(uid, name, !blocked)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun blockOrUnblock(uid: String, name: String, block: Boolean) {
        // Use set with merge — this works whether the 'blocked' field exists or not
        db.collection("users").document(uid)
            .set(mapOf("blocked" to block), SetOptions.merge())
            .addOnSuccessListener {
                val msg = if (block) "🔒 $name has been blocked" else "🔓 $name has been unblocked"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                loadUsers()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    "Failed to ${if (block) "block" else "unblock"} $name: ${e.message}",
                    Toast.LENGTH_LONG).show()
            }
    }
}
