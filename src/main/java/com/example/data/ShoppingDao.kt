package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {

    // --- ShoppingList ---
    @Query("SELECT * FROM shopping_lists ORDER BY createdAt DESC")
    fun getAllLists(): Flow<List<ShoppingList>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ShoppingList): Long

    @Delete
    suspend fun deleteList(list: ShoppingList)

    @Query("UPDATE shopping_lists SET name = :name WHERE id = :listId")
    suspend fun updateListName(listId: Int, name: String)


    // --- TemplateProduct ---
    @Query("SELECT * FROM template_products WHERE listId = :listId ORDER BY name ASC")
    fun getProductsForList(listId: Int): Flow<List<TemplateProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: TemplateProduct): Long

    @Update
    suspend fun updateProduct(product: TemplateProduct)

    @Delete
    suspend fun deleteProduct(product: TemplateProduct)

    @Query("UPDATE template_products SET lastPrice = :lastPrice WHERE id = :productId")
    suspend fun updateProductLastPrice(productId: Int, lastPrice: Double)

    @Query("UPDATE template_products SET isSelectedForTrip = :isSelected, quantity = :quantity WHERE id = :productId")
    suspend fun updateTemplateSelectionAndQuantity(productId: Int, isSelected: Boolean, quantity: Int)

    @Query("UPDATE template_products SET isSelectedForTrip = :isSelected WHERE listId = :listId")
    suspend fun updateAllTemplateSelection(listId: Int, isSelected: Boolean)


    // --- ActiveTripItem ---
    @Query("SELECT * FROM active_trip_items WHERE listId = :listId")
    fun getActiveTripItems(listId: Int): Flow<List<ActiveTripItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActiveTripItems(items: List<ActiveTripItem>)

    @Update
    suspend fun updateActiveTripItem(item: ActiveTripItem)

    @Query("UPDATE active_trip_items SET isSelectedForTrip = :isSelected WHERE listId = :listId")
    suspend fun updateAllActiveTripSelection(listId: Int, isSelected: Boolean)

    @Query("DELETE FROM active_trip_items WHERE listId = :listId")
    suspend fun deleteActiveTripItemsForList(listId: Int)


    // --- PurchaseHistory ---
    @Query("SELECT * FROM purchase_history_groups ORDER BY timestamp DESC")
    fun getAllHistoryGroups(): Flow<List<PurchaseHistoryGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryGroup(group: PurchaseHistoryGroup): Long

    @Query("DELETE FROM purchase_history_groups WHERE id = :groupId")
    suspend fun deleteHistoryGroupById(groupId: Int)

    @Query("DELETE FROM purchase_history_items WHERE historyGroupId = :groupId")
    suspend fun deleteHistoryItemsByGroupId(groupId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItems(items: List<PurchaseHistoryItem>)

    @Query("SELECT * FROM purchase_history_items WHERE historyGroupId = :groupId ORDER BY name ASC")
    fun getHistoryItemsForGroup(groupId: Int): Flow<List<PurchaseHistoryItem>>

    // --- CustomTag ---
    @Query("SELECT * FROM custom_tags ORDER BY name ASC")
    fun getAllCustomTags(): Flow<List<CustomTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomTag(tag: CustomTag)

    @Delete
    suspend fun deleteCustomTag(tag: CustomTag)

    @Query("UPDATE template_products SET tag = 'Otros' WHERE tag = :deletedTag")
    suspend fun clearTagInTemplateProducts(deletedTag: String)

    @Query("UPDATE active_trip_items SET tag = 'Otros' WHERE tag = :deletedTag")
    suspend fun clearTagInActiveTripItems(deletedTag: String)

    @Query("UPDATE purchase_history_items SET tag = 'Otros' WHERE tag = :deletedTag")
    suspend fun clearTagInHistoryItems(deletedTag: String)

    // --- Backup & Restore Raw Operations ---
    @Query("SELECT * FROM shopping_lists")
    suspend fun getAllListsRaw(): List<ShoppingList>

    @Query("SELECT * FROM template_products")
    suspend fun getAllTemplateProductsRaw(): List<TemplateProduct>

    @Query("SELECT * FROM purchase_history_groups")
    suspend fun getAllHistoryGroupsRaw(): List<PurchaseHistoryGroup>

    @Query("SELECT * FROM purchase_history_items")
    suspend fun getAllHistoryItemsRaw(): List<PurchaseHistoryItem>

    @Query("SELECT * FROM custom_tags")
    suspend fun getAllCustomTagsRaw(): List<CustomTag>

    @Query("DELETE FROM shopping_lists")
    suspend fun deleteAllLists()

    @Query("DELETE FROM template_products")
    suspend fun deleteAllTemplateProducts()

    @Query("DELETE FROM active_trip_items")
    suspend fun deleteAllActiveTripItems()

    @Query("DELETE FROM purchase_history_groups")
    suspend fun deleteAllHistoryGroups()

    @Query("DELETE FROM purchase_history_items")
    suspend fun deleteAllHistoryItems()

    @Query("DELETE FROM custom_tags")
    suspend fun deleteAllCustomTags()
}
