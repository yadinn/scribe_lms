package com.example.scribelms.staff

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.scribelms.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.*
import java.util.concurrent.Executors

class StaffQrScanActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private val db = FirebaseFirestore.getInstance()
    private val executor = Executors.newSingleThreadExecutor()
    private var scanned = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_qr_scan)

        previewView = findViewById(R.id.previewView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            Toast.makeText(this, "📷 Camera permission required", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun startCamera() {
        val providerFuture = ProcessCameraProvider.getInstance(this)

        providerFuture.addListener({
            val provider = providerFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val scanner = BarcodeScanning.getClient()

            analysis.setAnalyzer(executor) { proxy ->
                if (scanned) { proxy.close(); return@setAnalyzer }

                val mediaImage = proxy.image ?: run { proxy.close(); return@setAnalyzer }

                val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (b in barcodes) {
                            val value = b.rawValue ?: continue
                            scanned = true
                            runOnUiThread { handleQr(value) }
                            break
                        }
                    }
                    .addOnCompleteListener { proxy.close() }
            }

            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun handleQr(content: String) {
        if (!content.startsWith("SCRIBE|")) {
            Toast.makeText(this, "❌ Invalid QR code", Toast.LENGTH_LONG).show()
            scanned = false
            return
        }

        val map = mutableMapOf<String, String>()
        for (p in content.split("|")) {
            if (p.contains("=")) {
                val kv = p.split("=", limit = 2)
                if (kv.size == 2) map[kv[0]] = kv[1]
            }
        }

        when {
            map.containsKey("REQUEST_ID") -> issueBook(map["REQUEST_ID"]!!)
            map.containsKey("RETURN_REQUEST_ID") -> completeReturn(map["RETURN_REQUEST_ID"]!!)
            else -> {
                Toast.makeText(this, "❓ Unknown QR data", Toast.LENGTH_LONG).show()
                scanned = false
            }
        }
    }

    // ✅ FIX: use holdDaysAfterFine from settings instead of hardcoded 7
    private fun issueBook(requestId: String) {
        val requestRef = db.collection("issue_requests").document(requestId)

        // verify it's actually an approved request
        requestRef.get().addOnSuccessListener { doc ->
            if (!doc.exists() || doc.getString("status") != "approved") {
                Toast.makeText(this, "⚠️ Request not found or not approved", Toast.LENGTH_LONG).show()
                scanned = false
                return@addOnSuccessListener
            }

            db.collection("settings").document("library").get().addOnSuccessListener { sDoc ->
                val holdDays = (sDoc.getLong("holdDaysAfterFine") ?: 7).toInt()

                val due = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, holdDays)
                    set(Calendar.HOUR_OF_DAY, 16)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.time

                val bookId = doc.getString("bookId") ?: run {
                    Toast.makeText(this, "📚 Book not found", Toast.LENGTH_SHORT).show()
                    scanned = false
                    return@addOnSuccessListener
                }
                val bookRef = db.collection("books").document(bookId)

                db.runTransaction { t ->
                    val copies = t.get(bookRef).getLong("availableCopies") ?: 0
                    if (copies <= 0) throw Exception("No copies available")
                    t.update(bookRef, "availableCopies", copies - 1)
                    t.update(requestRef, mapOf(
                        "status" to "issued",
                        "issuedAt" to Date(),
                        "returnDueDate" to due,
                        "returnRequested" to false,
                        "lateDays" to 0,
                        "fineAmount" to 0,
                        "finePaid" to false
                    ))
                }.addOnSuccessListener {
                    Toast.makeText(this, "✅ Book issued successfully", Toast.LENGTH_LONG).show()
                    finish()
                }.addOnFailureListener {
                    Toast.makeText(this, "Issue failed: ${it.message}", Toast.LENGTH_LONG).show()
                    scanned = false
                }
            }
        }
    }

    private fun completeReturn(requestId: String) {
        val requestRef = db.collection("issue_requests").document(requestId)

        requestRef.get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                Toast.makeText(this, "⚠️ Request not found", Toast.LENGTH_LONG).show()
                scanned = false
                return@addOnSuccessListener
            }

            val bookId = doc.getString("bookId") ?: return@addOnSuccessListener
            val dueDate = doc.getDate("returnDueDate") ?: Date()

            val now = Calendar.getInstance()
            val daysLate = ((now.timeInMillis - dueDate.time) / (1000 * 60 * 60 * 24))
                .toInt().coerceAtLeast(0)

            val bookRef = db.collection("books").document(bookId)

            db.collection("settings").document("library").get().addOnSuccessListener { sDoc ->
                val finePerDay = (sDoc.getLong("finePerDay") ?: 10).toInt()
                val fineAmount = daysLate * finePerDay

                db.runTransaction { t ->
                    val copies = t.get(bookRef).getLong("availableCopies") ?: 0
                    t.update(bookRef, "availableCopies", copies + 1)
                    t.update(
                        requestRef,
                        mapOf(
                            "status" to "returned",
                            "returnedAt" to now.time,
                            "lateDays" to daysLate,
                            "fineAmount" to fineAmount,
                            "returnRequested" to false
                        )
                    )
                }.addOnSuccessListener {
                    val msg = if (fineAmount > 0)
                        "✅ Returned. Fine: ₹$fineAmount ($daysLate days late)"
                    else
                        "✅ Book returned on time! No fine."
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
}
