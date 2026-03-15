package com.example.scribelms.student.modeldata

data class Book(
    val bookId: String = "",
    val title: String = "",
    val author: String = "",
    val category: String = "",
    val description: String = "",
    val isbn: String = "",
    val totalCopies: Int = 0,
    val availableCopies: Int = 0,
    val bookImageUrl: String = ""
) {

    /**
     * Returns true only if at least one copy is available
     */
    fun isAvailable(): Boolean {
        return availableCopies > 0
    }

    /**
     * Returns readable status based on availability
     * (prevents mismatch bugs)
     */
    fun getStatus(): String {
        return if (availableCopies > 0) {
            "Available"
        } else {
            "Not Available"
        }
    }
}
