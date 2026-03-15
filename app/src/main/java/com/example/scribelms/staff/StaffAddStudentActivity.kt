package com.example.scribelms.staff

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.scribelms.R
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class StaffAddStudentActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etAdmissionNo: EditText
    private lateinit var etRegisterNo: EditText
    private lateinit var etYear: EditText
    private lateinit var etBranch: EditText
    private lateinit var etContact: EditText
    private lateinit var btnSave: Button

    private val db = FirebaseFirestore.getInstance()

    private var mode = "add"
    private var studentUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_student)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etAdmissionNo = findViewById(R.id.etAdmissionNo)
        etRegisterNo = findViewById(R.id.etRegisterNo)
        etYear = findViewById(R.id.etYear)
        etBranch = findViewById(R.id.etBranch)
        etContact = findViewById(R.id.etContact)
        btnSave = findViewById(R.id.btnSaveStudent)

        mode = intent.getStringExtra("mode") ?: "add"
        studentUid = intent.getStringExtra("studentUid")

        if (mode == "edit" && studentUid != null) {
            etPassword.visibility = EditText.GONE
            etEmail.isEnabled = false
            loadStudent(studentUid!!)
        }

        btnSave.setOnClickListener {
            if (mode == "edit") {
                updateStudent()
            } else {
                addStudentSafely()
            }
        }
    }

    /* ---------------- ADD STUDENT ---------------- */

    private fun addStudentSafely() {

        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val contact = etContact.text.toString().trim()

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || contact.isEmpty()) {
            Toast.makeText(this, "⚠️ All fields required", Toast.LENGTH_SHORT).show()
            return
        }

        val tempApp = FirebaseApp.initializeApp(
            this,
            FirebaseApp.getInstance().options,
            "TempStudentApp"
        )

        val tempAuth = FirebaseAuth.getInstance(tempApp)

        tempAuth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->

                val uid = result.user!!.uid

                val userData = hashMapOf(
                    "name" to name,
                    "email" to email,
                    "contact" to contact,
                    "role" to "student",
                    "blocked" to false,
                    "admissionNo" to etAdmissionNo.text.toString(),
                    "registerNo" to etRegisterNo.text.toString(),
                    "year" to etYear.text.toString(),
                    "branch" to etBranch.text.toString(),
                    "createdAt" to Date()
                )

                db.collection("users")
                    .document(uid)
                    .set(userData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "✅ Student added", Toast.LENGTH_SHORT).show()
                        tempAuth.signOut()
                        FirebaseApp.getInstance("TempStudentApp").delete()
                        finish()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
    }

    /* ---------------- LOAD STUDENT (EDIT) ---------------- */

    private fun loadStudent(uid: String) {
        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    etName.setText(doc.getString("name") ?: "")
                    etEmail.setText(doc.getString("email") ?: "")
                    etContact.setText(doc.getString("contact") ?: "")
                    etAdmissionNo.setText(doc.getString("admissionNo") ?: "")
                    etRegisterNo.setText(doc.getString("registerNo") ?: "")
                    etYear.setText(doc.getString("year") ?: "")
                    etBranch.setText(doc.getString("branch") ?: "")
                }
            }
    }

    /* ---------------- UPDATE STUDENT ---------------- */

    private fun updateStudent() {

        val uid = studentUid ?: return

        val updatedData = mapOf(
            "name" to etName.text.toString().trim(),
            "contact" to etContact.text.toString().trim(),
            "admissionNo" to etAdmissionNo.text.toString(),
            "registerNo" to etRegisterNo.text.toString(),
            "year" to etYear.text.toString(),
            "branch" to etBranch.text.toString()
        )

        db.collection("users")
            .document(uid)
            .update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(this, "✅ Student updated", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}