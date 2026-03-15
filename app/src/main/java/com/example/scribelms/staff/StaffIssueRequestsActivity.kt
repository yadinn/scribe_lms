package com.example.scribelms.staff

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.scribelms.R
import com.example.scribelms.adapter.IssueRequestAdapter
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class StaffIssueRequestsActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var adapter: IssueRequestAdapter
    private val requestList = mutableListOf<Map<String, Any>>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_staff_issue_requests)

        rvRequests = findViewById(R.id.rvRequests)
        rvRequests.layoutManager = LinearLayoutManager(this)

        adapter = IssueRequestAdapter(
            requestList,
            onApproveClick = { approveRequest(it) },
            onRejectClick = { rejectRequest(it) },
            onCollectedClick = { markAsCollected(it) },
            onReturnApproveClick = { approveReturn(it) }
        )

        rvRequests.adapter = adapter
        autoExpireApprovedRequests()
        fetchRequests()
    }

    override fun onResume() { super.onResume(); fetchRequests() }

    /* ---------------- SAFE LOAD ---------------- */

    private fun fetchRequests() {
        if (isFinishing || isDestroyed) return
        db.collection("issue_requests")
            .get()
            .addOnSuccessListener { result ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener

                requestList.clear()

                for (doc in result) {
                    val status = doc.getString("status") ?: ""
                    val returnRequested = doc.getBoolean("returnRequested") ?: false

                    if (
                        status == "pending" ||
                        status == "approved" ||
                        (status == "issued" && returnRequested)
                    ) {
                        requestList.add(doc.data + mapOf("requestId" to doc.id))
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }

    /* ---------------- APPROVE REQUEST ---------------- */

    private fun approveRequest(request: Map<String, Any>) {
        val studentUid = request["studentUid"] as String

        db.collection("issue_requests")
            .whereEqualTo("studentUid", studentUid)
            .whereEqualTo("status", "returned")
            .get()
            .addOnSuccessListener { snap ->

                val hasUnpaidFine = snap.any {
                    (it.getLong("fineAmount") ?: 0) > 0 &&
                            it.getBoolean("finePaid") == false
                }

                if (hasUnpaidFine) {
                    Toast.makeText(this, "Unpaid fine exists", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val ref = db.collection("issue_requests")
                    .document(request["requestId"] as String)

                val expiry = Calendar.getInstance().apply {
                    add(Calendar.HOUR_OF_DAY, 24)
                }.time

                ref.update(
                    mapOf(
                        "status" to "approved",
                        "approvedAt" to Date(),
                        "collectionExpiry" to expiry
                    )
                ).addOnSuccessListener {
                    fetchRequests()
                }
            }
    }

    /* ---------------- COLLECT ---------------- */

    private fun markAsCollected(request: Map<String, Any>) {

        val settingsRef = db.collection("settings").document("library")
        val bookId = request["bookId"] as? String ?: return
        val bookRef = db.collection("books").document(bookId)
        val requestRef = db.collection("issue_requests").document(request["requestId"] as String)

        settingsRef.get().addOnSuccessListener { doc ->
            val days = (doc.getLong("holdDaysAfterFine") ?: 7).toInt()

            val due = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, days)
                set(Calendar.HOUR_OF_DAY, 16)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.time

            // ✅ FIX: decrement availableCopies in transaction
            db.runTransaction { t ->
                val copies = t.get(bookRef).getLong("availableCopies") ?: 0
                if (copies <= 0) throw Exception("No copies available")
                t.update(bookRef, "availableCopies", copies - 1)
                t.update(requestRef, mapOf(
                    "status" to "issued",
                    "issuedAt" to Date(),
                    "returnDueDate" to due,
                    "lateDays" to 0,
                    "fineAmount" to 0,
                    "finePaid" to false,
                    "returnRequested" to false
                ))
            }.addOnSuccessListener { fetchRequests() }
              .addOnFailureListener { e ->
                  Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
              }
        }
    }

    /* ---------------- RETURN APPROVAL ---------------- */

    private fun approveReturn(request: Map<String, Any>) {
        val requestId = request["requestId"] as String
        val bookId = request["bookId"] as String

        val now = Calendar.getInstance()
        if (now.get(Calendar.HOUR_OF_DAY) >= 16) {
            now.add(Calendar.DATE, 1)
            now.set(Calendar.HOUR_OF_DAY, 10)
        }

        calculateFineAndReturn(requestId, bookId, now.time)
    }

    private fun calculateFineAndReturn(
        requestId: String,
        bookId: String,
        finalReturnTime: Date
    ) {

        val settingsRef = db.collection("settings").document("library")
        val requestRef = db.collection("issue_requests").document(requestId)
        val bookRef = db.collection("books").document(bookId)

        settingsRef.get().addOnSuccessListener { sDoc ->
            val finePerDay = (sDoc.getLong("finePerDay") ?: 10).toInt()

            requestRef.get().addOnSuccessListener { rDoc ->
                val due = rDoc.getDate("returnDueDate") ?: return@addOnSuccessListener

                val calDue = Calendar.getInstance().apply {
                    time = due
                    set(Calendar.HOUR_OF_DAY, 16)
                }

                val diff =
                    ((finalReturnTime.time - calDue.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

                val lateDays = maxOf(diff, 0)
                val fineAmount = lateDays * finePerDay

                db.runTransaction { t ->
                    val copies = t.get(bookRef).getLong("availableCopies") ?: 0
                    t.update(bookRef, "availableCopies", copies + 1)

                    t.update(
                        requestRef,
                        mapOf(
                            "status" to "returned",
                            "returnedAt" to finalReturnTime,
                            "lateDays" to lateDays,
                            "fineAmount" to fineAmount,
                            "finePaid" to false,
                            "returnRequested" to false
                        )
                    )
                }.addOnSuccessListener { fetchRequests() }
            }
        }
    }

    /* ---------------- REJECT ---------------- */

    private fun rejectRequest(request: Map<String, Any>) {
        db.collection("issue_requests")
            .document(request["requestId"] as String)
            .update(
                mapOf(
                    "status" to "rejected",
                    "rejectedAt" to Date()
                )
            )
            .addOnSuccessListener { fetchRequests() }
    }

    /* ---------------- AUTO EXPIRE ---------------- */

    private fun autoExpireApprovedRequests() {
        val now = Date()

        db.collection("issue_requests")
            .whereEqualTo("status", "approved")
            .get()
            .addOnSuccessListener { result ->
                for (doc in result) {
                    val expiry = doc.getDate("collectionExpiry") ?: continue
                    if (!now.after(expiry)) continue

                    val bookId = doc.getString("bookId") ?: continue
                    val bookRef = db.collection("books").document(bookId)

                    db.runTransaction { t ->
                        val copies = t.get(bookRef).getLong("availableCopies") ?: 0
                        t.update(bookRef, "availableCopies", copies + 1)
                        t.update(doc.reference, "status", "expired")
                    }
                }
            }
    }
}