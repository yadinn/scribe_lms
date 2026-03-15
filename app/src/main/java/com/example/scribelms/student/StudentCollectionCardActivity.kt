package com.example.scribelms.student

import android.os.Bundle
import android.os.CountDownTimer
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.scribelms.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import android.graphics.Bitmap
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

class StudentCollectionCardActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var tvBook: TextView
    private lateinit var tvStudent: TextView
    private lateinit var tvRegister: TextView
    private lateinit var tvDateTime: TextView
    private lateinit var tvTimer: TextView
    private lateinit var ivQr: ImageView

    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_collection_card)

        tvBook = findViewById(R.id.tvBookTitle)
        tvStudent = findViewById(R.id.tvStudentName)
        tvRegister = findViewById(R.id.tvRegisterNo)
        tvDateTime = findViewById(R.id.tvDateTime)
        tvTimer = findViewById(R.id.tvTimer)
        ivQr = findViewById(R.id.ivQr)

        loadCard()
    }

    private fun loadCard() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("issue_requests")
            .whereEqualTo("studentUid", uid)
            .whereEqualTo("status", "approved")
            .limit(1)
            .get()
            .addOnSuccessListener { snap ->

                if (snap.isEmpty) {
                    Toast.makeText(this, "No approved request", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                val doc = snap.documents[0]
                val requestId = doc.id

                val bookTitle = doc.getString("bookTitle") ?: "-"
                val expiry = doc.getDate("collectionExpiry") ?: return@addOnSuccessListener

                // student details
                db.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                    val name = userDoc.getString("name") ?: "-"
                    val reg = userDoc.getString("registerNo") ?: "-"

                    tvBook.text = "📘 $bookTitle"
                    tvStudent.text = "👤 $name"
                    tvRegister.text = "🎓 Reg No: $reg"

                    val fmt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                    tvDateTime.text = "Approved at: ${fmt.format(doc.getDate("approvedAt") ?: Date())}"

                    startTimer(expiry)
                    generateQr(requestId, uid)
                }
            }
    }

    private fun startTimer(expiry: Date) {
        val diff = expiry.time - System.currentTimeMillis()

        if (diff <= 0) {
            tvTimer.text = "❌ Expired"
            return
        }

        timer?.cancel()
        timer = object : CountDownTimer(diff, 1000) {
            override fun onTick(ms: Long) {
                val totalSec = ms / 1000
                val h = totalSec / 3600
                val m = (totalSec % 3600) / 60
                val s = totalSec % 60
                tvTimer.text = "⏳ $h h $m m $s s"
            }

            override fun onFinish() {
                tvTimer.text = "❌ Expired"
            }
        }.start()
    }

    private fun generateQr(requestId: String, uid: String) {
        val content = "SCRIBE|REQUEST_ID=$requestId|STUDENT_UID=$uid"

        val matrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            600,
            600
        )

        val bmp = Bitmap.createBitmap(600, 600, Bitmap.Config.RGB_565)
        for (x in 0 until 600) {
            for (y in 0 until 600) {
                bmp.setPixel(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        ivQr.setImageBitmap(bmp)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}