package com.example.scribelms.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.scribelms.R
import com.example.scribelms.admin.AdminHomeActivity
import com.example.scribelms.staff.StaffHomeActivity
import com.example.scribelms.student.StudentHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db   = FirebaseFirestore.getInstance()

        val current = auth.currentUser
        if (current != null) {
            // Check blocked status and role before auto-login
            db.collection("users").document(current.uid).get()
                .addOnSuccessListener { doc ->
                    val role    = doc.getString("role") ?: ""
                    val blocked = doc.getBoolean("blocked") ?: false
                    if (role.isNotEmpty() && !blocked) {
                        redirectByRole(role)
                    } else {
                        if (blocked) auth.signOut()
                        showLoginUI()
                    }
                }
                .addOnFailureListener { showLoginUI() }
            return
        }

        showLoginUI()
    }

    private fun showLoginUI() {
        val etEmail    = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin   = findViewById<Button>(R.id.btnLogin)
        val spinner    = findViewById<Spinner>(R.id.spinnerRole)

        etEmail.visibility    = View.VISIBLE
        etPassword.visibility = View.VISIBLE
        btnLogin.visibility   = View.VISIBLE
        spinner.visibility    = View.VISIBLE

        val roles = listOf("admin", "staff", "student")
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, roles)

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val role     = spinner.selectedItem.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid ?: run {
                        btnLogin.isEnabled = true; return@addOnSuccessListener
                    }
                    db.collection("users").document(uid).get()
                        .addOnSuccessListener { doc ->
                            if (!doc.exists()) {
                                auth.signOut(); btnLogin.isEnabled = true
                                Toast.makeText(this, "User profile not found", Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }
                            val blocked = doc.getBoolean("blocked") ?: false
                            if (blocked) {
                                auth.signOut(); btnLogin.isEnabled = true
                                Toast.makeText(this, "Your account has been blocked. Contact admin.", Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }
                            val actualRole = doc.getString("role") ?: ""
                            if (actualRole != role) {
                                auth.signOut(); btnLogin.isEnabled = true
                                Toast.makeText(this, "Wrong login type selected", Toast.LENGTH_LONG).show()
                                return@addOnSuccessListener
                            }
                            redirectByRole(actualRole)
                        }
                        .addOnFailureListener {
                            auth.signOut(); btnLogin.isEnabled = true
                            Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener {
                    btnLogin.isEnabled = true
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun redirectByRole(role: String) {
        val cls = when (role) {
            "admin" -> AdminHomeActivity::class.java
            "staff" -> StaffHomeActivity::class.java
            else    -> StudentHomeActivity::class.java
        }
        startActivity(Intent(this, cls).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
