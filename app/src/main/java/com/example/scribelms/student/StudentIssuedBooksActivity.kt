package com.example.scribelms.student

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.IssuedBookAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.util.Date

class StudentIssuedBooksActivity : AppCompatActivity() {

    private lateinit var rvIssuedBooks: RecyclerView
    private lateinit var adapter: IssuedBookAdapter
    private val issuedList = mutableListOf<Map<String, Any>>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_issued_books)

        rvIssuedBooks = findViewById(R.id.rvIssuedBooks)
        rvIssuedBooks.layoutManager = LinearLayoutManager(this)

        adapter = IssuedBookAdapter(
            issuedList,
            onReturnClick = { requestReturn(it) }
        )

        rvIssuedBooks.adapter = adapter
        fetchIssuedBooks()
    }

    // 📚 FETCH ISSUED BOOKS
    private fun fetchIssuedBooks() {

        val uid = auth.currentUser?.uid ?: return

        db.collection("issue_requests")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("status", "issued")
            .get()
            .addOnSuccessListener { result ->

                issuedList.clear()

                for (doc in result) {
                    val data = doc.data.toMutableMap()
                    data["requestId"] = doc.id

                    // 🔥 ENSURE DATE FIELD EXISTS
                    if (!data.containsKey("returnDueDate")) {
                        continue
                    }

                    issuedList.add(data)
                }

                adapter.notifyDataSetChanged()
            }
    }

    // 🔁 STUDENT → REQUEST RETURN
    private fun requestReturn(request: Map<String, Any>) {

        val requestId = request["requestId"] as? String
        val studentUid = auth.currentUser?.uid

        if (requestId == null || studentUid == null) {
            Toast.makeText(this, "Invalid request", Toast.LENGTH_SHORT).show()
            return
        }

        val requestRef = db.collection("issue_requests").document(requestId)

        requestRef.get().addOnSuccessListener { doc ->

            if (!doc.exists()) {
                Toast.makeText(this, "Request not found", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val status = doc.getString("status")
            val ownerUid = doc.getString("studentUid")

            if (status != "issued" || ownerUid != studentUid) {
                Toast.makeText(this, "Invalid return action", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            requestRef.update(
                mapOf(
                    "returnRequested" to true,
                    "returnRequestedAt" to Date()
                )
            ).addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Return requested. Show QR to staff",
                    Toast.LENGTH_SHORT
                ).show()

                showReturnQr(requestId)
                fetchIssuedBooks()

            }.addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to request return: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // 📷 SHOW RETURN QR — fetch live fine amount first
    private fun showReturnQr(requestId: String) {

        val studentUid = auth.currentUser?.uid ?: return

        db.collection("issue_requests").document(requestId).get()
            .addOnSuccessListener { doc ->
                val fineAmount = (doc.getLong("fineAmount") ?: 0).toInt()
                val lateDays = (doc.getLong("lateDays") ?: 0).toInt()

                val qrData = "SCRIBE|RETURN_REQUEST_ID=$requestId|STUDENT_UID=$studentUid"
                val bitMatrix = QRCodeWriter().encode(qrData, BarcodeFormat.QR_CODE, 600, 600)
                val bitmap = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565)
                for (x in 0 until 600)
                    for (y in 0 until 600)
                        bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)

                val dialog = android.app.Dialog(this)
                dialog.setContentView(R.layout.dialog_qr)
                dialog.findViewById<android.widget.ImageView>(R.id.ivQr).setImageBitmap(bitmap)

                val tvFineInfo = dialog.findViewById<android.widget.TextView>(R.id.tvFineInfo)
                if (fineAmount > 0) {
                    tvFineInfo.visibility = android.view.View.VISIBLE
                    tvFineInfo.text = "⚠️ Fine: ₹$fineAmount ($lateDays day(s) late) — pay at desk"
                } else {
                    tvFineInfo.visibility = android.view.View.GONE
                }
                dialog.show()
            }
    }
}
