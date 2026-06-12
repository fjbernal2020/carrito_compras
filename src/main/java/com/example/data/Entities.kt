package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "template_products")
data class TemplateProduct(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listId: Int,
    val name: String,
    val quantity: Int = 1,
    val tag: String, // e.g. "Frutería", "Lácteos", etc.
    val lastPrice: Double = 0.0,
    val isSelectedForTrip: Boolean = true
)

@Entity(tableName = "active_trip_items")
data class ActiveTripItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listId: Int,
    val templateProductId: Int,
    val name: String,
    val quantity: Int,
    val tag: String,
    val price: Double,
    val isBought: Boolean = false,
    val isSelectedForTrip: Boolean = true
)

@Entity(tableName = "purchase_history_groups")
data class PurchaseHistoryGroup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val totalAmount: Double
)

@Entity(tableName = "purchase_history_items")
data class PurchaseHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val historyGroupId: Int,
    val name: String,
    val quantity: Int,
    val price: Double,
    val tag: String
)

@Entity(tableName = "custom_tags")
data class CustomTag(
    @PrimaryKey val name: String,
    val emoji: String = "🏷️",
    val colorLightHex: String = "#F5F5F5",
    val colorDarkHex: String = "#303030"
)
