package com.example.scribelms.staff

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.scribelms.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StaffEditProfileActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_edit_profile)

        val etName    = findViewById<EditText>(R.id.etName)
        val etEmail   = findViewById<EditText>(R.id.etEmail)
        val etContact = findViewById<EditText>(R.id.etContact)
        val btnSave   = findViewById<Button>(R.id.btnSaveProfile)

        etEmail.isEnabled = false
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            etName.setText(doc.getString("name") ?: "")
            etEmail.setText(doc.getString("email") ?: "")
            etContact.setText(doc.getString("contact") ?: "")
        }

        btnSave.setOnClickListener {
            val name    = etName.text.toString().trim()
            val contact = etContact.text.toString().trim()
            if (name.isEmpty()) { etName.error = "Required"; return@setOnClickListener }
            db.collection("users").document(uid)
                .update(mapOf("name" to name, "contact" to contact))
                .addOnSuccessListener { Toast.makeText(this, "✅ Profile updated", Toast.LENGTH_SHORT).show(); finish() }
                .addOnFailureListener { Toast.makeText(this, "❌ Failed: ${it.message}", Toast.LENGTH_SHORT).show() }
        }
    }
}
