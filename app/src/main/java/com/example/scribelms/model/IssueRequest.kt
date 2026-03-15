package com.example.scribelms.model

import com.google.firebase.Timestamp

data class IssueRequest(
    val requestId: String = "",

    val bookId: String = "",
    val bookTitle: String = "",

    val studentUid: String = "",
    val studentName: String = "",
    val studentRegNo: String = "",     // ✅ FIX
    val studentContact: String = "",   // (future-safe)

    val status: String = "",

    val requestedAt: Timestamp? = null,
    val approvedAt: Timestamp? = null,
    val issuedAt: Timestamp? = null,
    val collectionExpiry: Timestamp? = null,

    val returnDueDate: Timestamp? = null,
    val returnedAt: Timestamp? = null,

    val returnRequested: Boolean = false,
    val returnRequestedAt: Timestamp? = null,

    val lateDays: Int = 0,
    val fineAmount: Int = 0,
    val finePaid: Boolean = false
)