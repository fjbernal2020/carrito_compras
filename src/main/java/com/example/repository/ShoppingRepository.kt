package com.example.repository

import com.example.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ShoppingRepository(private val shoppingDao: ShoppingDao) {

    // --- ShoppingList ---
    val allLists: Flow<List<ShoppingList>> = shoppingDao.getAllLists()

    suspend fun insertList(list: ShoppingList): Long {
        return shoppingDao.insertList(list)
    }

    suspend fun deleteList(list: ShoppingList) {
        shoppingDao.deleteList(list)
    }

    suspend fun updateListName(listId: Int, name: String) {
        shoppingDao.updateListName(listId, name)
    }


    // --- TemplateProduct ---
    fun getProductsForList(listId: Int): Flow<List<TemplateProduct>> {
        return shoppingDao.getProductsForList(listId)
    }

    suspend fun insertProduct(product: TemplateProduct): Long {
        return shoppingDao.insertProduct(product)
    }

    suspend fun updateProduct(product: TemplateProduct) {
        shoppingDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: TemplateProduct) {
        shoppingDao.deleteProduct(product)
    }

    suspend fun updateProductLastPrice(productId: Int, lastPrice: Double) {
        shoppingDao.updateProductLastPrice(productId, lastPrice)
    }

    suspend fun updateTemplateSelectionAndQuantity(productId: Int, isSelected: Boolean, quantity: Int) {
        shoppingDao.updateTemplateSelectionAndQuantity(productId, isSelected, quantity)
    }

    suspend fun updateAllTemplateSelection(listId: Int, isSelected: Boolean) {
        shoppingDao.updateAllTemplateSelection(listId, isSelected)
    }


    // --- ActiveTripItem ---
    fun getActiveTripItems(listId: Int): Flow<List<ActiveTripItem>> {
        return shoppingDao.getActiveTripItems(listId)
    }

    suspend fun insertActiveTripItems(items: List<ActiveTripItem>) {
        shoppingDao.insertActiveTripItems(items)
    }

    suspend fun updateActiveTripItem(item: ActiveTripItem) {
        shoppingDao.updateActiveTripItem(item)
    }

    suspend fun updateAllActiveTripSelection(listId: Int, isSelected: Boolean) {
        shoppingDao.updateAllActiveTripSelection(listId, isSelected)
    }

    suspend fun deleteActiveTripItemsForList(listId: Int) {
        shoppingDao.deleteActiveTripItemsForList(listId)
    }


    // --- PurchaseHistory ---
    val allHistoryGroups: Flow<List<PurchaseHistoryGroup>> = shoppingDao.getAllHistoryGroups()

    suspend fun insertHistoryGroup(group: PurchaseHistoryGroup): Long {
        return shoppingDao.insertHistoryGroup(group)
    }

    suspend fun deleteHistoryGroup(groupId: Int) {
        shoppingDao.deleteHistoryItemsByGroupId(groupId)
        shoppingDao.deleteHistoryGroupById(groupId)
    }

    suspend fun insertHistoryItems(items: List<PurchaseHistoryItem>) {
        shoppingDao.insertHistoryItems(items)
    }

    fun getHistoryItemsForGroup(groupId: Int): Flow<List<PurchaseHistoryItem>> {
        return shoppingDao.getHistoryItemsForGroup(groupId)
    }

    // --- CustomTag ---
    val allCustomTags: Flow<List<CustomTag>> = shoppingDao.getAllCustomTags()

    suspend fun insertCustomTag(tag: CustomTag) {
        shoppingDao.insertCustomTag(tag)
    }

    suspend fun deleteCustomTag(tag: CustomTag) {
        shoppingDao.deleteCustomTag(tag)
        shoppingDao.clearTagInTemplateProducts(tag.name)
        shoppingDao.clearTagInActiveTripItems(tag.name)
        shoppingDao.clearTagInHistoryItems(tag.name)
    }

    suspend fun initializeDefaultTags(force: Boolean = false) {
        val currentList = shoppingDao.getAllCustomTags().first()
        val isEmpty = currentList.isEmpty()
        if (isEmpty || force) {
            val defaults = listOf(
                CustomTag("Alimentación", "🥫", "#FFF3E0", "#4E3629"),
                CustomTag("Frutería", "🍎", "#E8F5E9", "#1B3B22"),
                CustomTag("Carnicería", "🥩", "#FFFFEBEE", "#4A151B"),
                CustomTag("Pescadería", "🐟", "#E3F2FD", "#0D324D"),
                CustomTag("Lácteos", "🥛", "#FFFDE7", "#3E3B1C"),
                CustomTag("Congelados", "❄️", "#E0F7FA", "#00363A"),
                CustomTag("Limpieza", "🧼", "#F3E5F5", "#381A3C"),
                CustomTag("Perfumería", "🧴", "#EDE7F6", "#2A1B40"),
                CustomTag("Bebidas", "🥤", "#ECEFF1", "#263238"),
                CustomTag("Panadería", "🍞", "#FFF8E1", "#4E3E20"),
                CustomTag("Mascotas", "🐶", "#EFEBE9", "#3E2723"),
                CustomTag("Otros", "📦", "#F5F5F5", "#303030")
            )
            for (tag in defaults) {
                shoppingDao.insertCustomTag(tag)
            }
        }
    }

    // --- Backup & Restore Raw Operations ---
    suspend fun getAllListsRaw() = shoppingDao.getAllListsRaw()
    suspend fun getAllTemplateProductsRaw() = shoppingDao.getAllTemplateProductsRaw()
    suspend fun getAllHistoryGroupsRaw() = shoppingDao.getAllHistoryGroupsRaw()
    suspend fun getAllHistoryItemsRaw() = shoppingDao.getAllHistoryItemsRaw()
    suspend fun getAllCustomTagsRaw() = shoppingDao.getAllCustomTagsRaw()

    suspend fun insertCustomTagDirect(tag: CustomTag) = shoppingDao.insertCustomTag(tag)

    suspend fun clearDatabase() {
        shoppingDao.deleteAllActiveTripItems()
        shoppingDao.deleteAllTemplateProducts()
        shoppingDao.deleteAllLists()
        shoppingDao.deleteAllHistoryItems()
        shoppingDao.deleteAllHistoryGroups()
        shoppingDao.deleteAllCustomTags()
    }
}
