package com.example.scribelms.admin

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scribelms.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AdminSettingsActivity : AppCompatActivity() {

    private lateinit var etMaxIssueLimit: EditText
    private lateinit var etHoldDays: EditText
    // ✅ FIX: etFinePerDay now actually read and saved
    private lateinit var etFinePerDay: EditText
    private lateinit var btnSaveSettings: Button

    private val db = FirebaseFirestore.getInstance()
    private val settingsRef = db.collection("settings").document("library")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_settings)

        etMaxIssueLimit = findViewById(R.id.etMaxIssueLimit)
        etHoldDays = findViewById(R.id.etHoldDays)
        etFinePerDay = findViewById(R.id.etFinePerDay)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)

        loadSettings()

        btnSaveSettings.setOnClickListener { saveSettings() }
    }

    private fun loadSettings() {
        settingsRef.get().addOnSuccessListener { doc ->
            etMaxIssueLimit.setText((doc.getLong("maxIssueLimit") ?: 3).toString())
            etHoldDays.setText((doc.getLong("holdDaysAfterFine") ?: 7).toString())
            // ✅ FIX: load finePerDay too
            etFinePerDay.setText((doc.getLong("finePerDay") ?: 10).toString())
        }
    }

    private fun saveSettings() {
        val maxIssue = etMaxIssueLimit.text.toString().toIntOrNull()
        val holdDays = etHoldDays.text.toString().toIntOrNull()
        // ✅ FIX: save finePerDay
        val finePerDay = etFinePerDay.text.toString().toIntOrNull()

        if (maxIssue == null || maxIssue <= 0) {
            etMaxIssueLimit.error = "Enter a valid number"
            return
        }
        if (holdDays == null || holdDays <= 0) {
            etHoldDays.error = "Enter a valid number"
            return
        }
        if (finePerDay == null || finePerDay < 0) {
            etFinePerDay.error = "Enter a valid amount (0 or more)"
            return
        }

        settingsRef.set(
            mapOf(
                "maxIssueLimit" to maxIssue,
                "holdDaysAfterFine" to holdDays,
                "finePerDay" to finePerDay   // ✅ FIX: actually saved now
            ),
            SetOptions.merge()
        ).addOnSuccessListener {
            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to save: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }
}
