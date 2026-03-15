package com.example.scribelms.admin

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.scribelms.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class AdminAddStaffActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_staff)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnSave = findViewById(R.id.btnSaveStaff)

        btnSave.setOnClickListener {
            addStaff()
        }
    }

    private fun addStaff() {

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "⚠️ All fields required", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔐 TEMP AUTH
        val tempApp = FirebaseApp.initializeApp(
            this,
            FirebaseApp.getInstance().options,
            "TempStaffApp"
        )

        val tempAuth = FirebaseAuth.getInstance(tempApp)

        tempAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val uid = result.user!!.uid

                val data = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "role" to "staff",
                    "blocked" to false,
                    "createdAt" to Date()
                )

                db.collection("users")
                    .document(uid)
                    .set(data)
                    .addOnSuccessListener {

                        Toast.makeText(
                            this,
                            "Staff added successfully",
                            Toast.LENGTH_SHORT
                        ).show()

                        tempAuth.signOut()
                        FirebaseApp.getInstance("TempStaffApp").delete()

                        finish()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
    }
}
