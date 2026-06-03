package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val vendor: String,
    val amount: Double,
    val category: String, // "Food", "Utility", "Subscriptions", "Others"
    val dateLong: Long, // timestamp
    val docName: String = "" // extracted invoice name or path reference
)

enum class ExpenseCategory(val categoryName: String, val colorHex: String) {
    FOOD("Food", "#FF7043"),          // Deep Orange
    UTILITY("Utility", "#26A69A"),      // Teal
    SUBSCRIPTIONS("Subscriptions", "#5C6BC0"), // Indigo
    OTHERS("Others", "#78909C");         // Blue Grey

    companion object {
        fun fromString(value: String): ExpenseCategory {
            return entries.firstOrNull { it.categoryName.equals(value, ignoreCase = true) } ?: OTHERS
        }
    }
}
