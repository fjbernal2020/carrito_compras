package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.repository.ShoppingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface Screen {
    object Dashboard : Screen
    data class ListEditor(val listId: Int, val listName: String) : Screen
    data class Planning(val listId: Int, val listName: String) : Screen
    data class SupermarketMode(val listId: Int, val listName: String) : Screen
    data class HistoryDetails(val groupId: Int, val listName: String, val timestamp: Long, val total: Double) : Screen
}

class ShoppingViewModel(
    application: Application,
    private val repository: ShoppingRepository
) : AndroidViewModel(application) {

    init {
        viewModelScope.launch {
            repository.initializeDefaultTags()
        }
    }

    // --- Dynamic Tags States ---
    val allCustomTags: StateFlow<List<CustomTag>> = repository.allCustomTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private suspend fun checkAndInsertCustomTag(tagName: String) {
        val trimmed = tagName.trim()
        if (trimmed.isEmpty()) return
        val currentTags = repository.allCustomTags.first()
        val exists = currentTags.any { it.name.equals(trimmed, ignoreCase = true) }
        if (!exists) {
            repository.insertCustomTag(
                CustomTag(
                    name = trimmed,
                    emoji = "🏷️",
                    colorLightHex = "#E8F5E9",
                    colorDarkHex = "#1B3B22"
                )
            )
        }
    }

    fun addCustomTagDirectly(name: String, emoji: String) {
        viewModelScope.launch {
            repository.insertCustomTag(
                CustomTag(
                    name = name.trim(),
                    emoji = emoji,
                    colorLightHex = "#E8F5E9",
                    colorDarkHex = "#1B3B22"
                )
            )
        }
    }

    fun deleteCustomTag(tag: CustomTag) {
        viewModelScope.launch {
            repository.deleteCustomTag(tag)
        }
    }

    // --- Navigation State ---
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Dashboard)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    // --- Dashboard States ---
    val allLists: StateFlow<List<ShoppingList>> = repository.allLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHistoryGroups: StateFlow<List<PurchaseHistoryGroup>> = repository.allHistoryGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dynamic screen flows ---
    private val _selectedListId = MutableStateFlow<Int?>(null)

    val currentTemplateProducts: StateFlow<List<TemplateProduct>> = _selectedListId
        .flatMapLatest { id ->
            if (id != null) repository.getProductsForList(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentActiveTripItems: StateFlow<List<ActiveTripItem>> = _selectedListId
        .flatMapLatest { id ->
            if (id != null) repository.getActiveTripItems(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedHistoryGroupId = MutableStateFlow<Int?>(null)
    val currentHistoryItems: StateFlow<List<PurchaseHistoryItem>> = _selectedHistoryGroupId
        .flatMapLatest { id ->
            if (id != null) repository.getHistoryItemsForGroup(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun selectList(listId: Int) {
        _selectedListId.value = listId
    }

    fun selectHistoryGroup(groupId: Int) {
        _selectedHistoryGroupId.value = groupId
    }

    // --- ShoppingList Actions ---
    fun createList(name: String) {
        viewModelScope.launch {
            repository.insertList(ShoppingList(name = name))
        }
    }

    fun deleteList(list: ShoppingList) {
        viewModelScope.launch {
            repository.deleteList(list)
            repository.deleteActiveTripItemsForList(list.id)
        }
    }

    fun updateListName(listId: Int, name: String) {
        viewModelScope.launch {
            repository.updateListName(listId, name)
        }
    }

    // --- TemplateProduct Actions ---
    fun addTemplateProduct(listId: Int, name: String, quantity: Int, tag: String, lastPrice: Double) {
        viewModelScope.launch {
            checkAndInsertCustomTag(tag)
            repository.insertProduct(
                TemplateProduct(
                    listId = listId,
                    name = name,
                    quantity = quantity,
                    tag = tag,
                    lastPrice = lastPrice
                )
            )
        }
    }

    fun updateTemplateProduct(product: TemplateProduct) {
        viewModelScope.launch {
            checkAndInsertCustomTag(product.tag)
            repository.updateProduct(product)
        }
    }

    fun deleteTemplateProduct(product: TemplateProduct) {
        viewModelScope.launch {
            repository.deleteProduct(product)
        }
    }

    // --- ActiveTripActions (Planning & Buying) ---
    fun prepareAndStartPlanning(listId: Int) {
        viewModelScope.launch {
            selectList(listId)
            // Retrieve current template products of this list
            val templates = repository.getProductsForList(listId).first()
            // Clear past active items and initialize new ones
            repository.deleteActiveTripItemsForList(listId)
            val items = templates.map {
                ActiveTripItem(
                    listId = listId,
                    templateProductId = it.id,
                    name = it.name,
                    quantity = it.quantity,
                    tag = it.tag,
                    price = it.lastPrice,
                    isBought = false,
                    isSelectedForTrip = it.isSelectedForTrip
                )
            }
            repository.insertActiveTripItems(items)
        }
    }

    fun updateActiveTripItem(item: ActiveTripItem) {
        viewModelScope.launch {
            repository.updateActiveTripItem(item)
            // Synchronize selection state and quantity to the original template product
            repository.updateTemplateSelectionAndQuantity(
                productId = item.templateProductId,
                isSelected = item.isSelectedForTrip,
                quantity = item.quantity
            )
            // Also, if price changed and is updated, update the template product's last price right away
            if (item.isSelectedForTrip) {
                repository.updateProductLastPrice(item.templateProductId, item.price)
            }
        }
    }

    fun selectAllActiveTripItems(listId: Int) {
        viewModelScope.launch {
            repository.updateAllActiveTripSelection(listId, true)
            repository.updateAllTemplateSelection(listId, true)
        }
    }

    fun createActiveTripItemManually(listId: Int, name: String, quantity: Int, tag: String, price: Double) {
        viewModelScope.launch {
            checkAndInsertCustomTag(tag)
            // Create a template item first, then make it an active item.
            val templateId = repository.insertProduct(
                TemplateProduct(
                    listId = listId,
                    name = name,
                    quantity = quantity,
                    tag = tag,
                    lastPrice = price
                )
            ).toInt()

            val activeItem = ActiveTripItem(
                listId = listId,
                templateProductId = templateId,
                name = name,
                quantity = quantity,
                tag = tag,
                price = price,
                isBought = false,
                isSelectedForTrip = true
            )
            repository.insertActiveTripItems(listOf(activeItem))
        }
    }

    // Finish shopping and save history
    fun finalizePurchase(listId: Int, listName: String) {
        viewModelScope.launch {
            val items = repository.getActiveTripItems(listId).first()
            val boughtItems = items.filter { it.isSelectedForTrip && it.isBought }
            if (boughtItems.isEmpty()) {
                // If they didn't buy anything, simply delete active trip and return
                repository.deleteActiveTripItemsForList(listId)
                _currentScreen.value = Screen.Dashboard
                return@launch
            }

            val totalAmount = boughtItems.sumOf { it.quantity * it.price }

            // 1. Create history group
            val groupId = repository.insertHistoryGroup(
                PurchaseHistoryGroup(
                    listName = listName,
                    totalAmount = totalAmount,
                    timestamp = System.currentTimeMillis()
                )
            ).toInt()

            // 2. Create history items
            val historyItems = boughtItems.map {
                PurchaseHistoryItem(
                    historyGroupId = groupId,
                    name = it.name,
                    quantity = it.quantity,
                    price = it.price,
                    tag = it.tag
                )
            }
            repository.insertHistoryItems(historyItems)

            // 3. Clear active items for this list
            repository.deleteActiveTripItemsForList(listId)

            // 4. Navigate back to Dashboard
            _currentScreen.value = Screen.Dashboard
        }
    }

    fun cancelPurchase(listId: Int) {
        viewModelScope.launch {
            repository.deleteActiveTripItemsForList(listId)
            _currentScreen.value = Screen.Dashboard
        }
    }

    fun deleteHistoryGroup(groupId: Int) {
        viewModelScope.launch {
            repository.deleteHistoryGroup(groupId)
        }
    }

    // --- Import / Export Backup Operations ---
    fun exportAllDataToJson(callback: (String) -> Unit) {
        viewModelScope.launch {
            val json = com.example.utils.BackupHelper.exportToJson(repository)
            callback(json)
        }
    }

    fun importAllDataFromJson(jsonString: String, overwrite: Boolean, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = com.example.utils.BackupHelper.importFromJson(jsonString, repository, overwrite)
            callback(result)
        }
    }

    fun exportAllDataToCsv(callback: (String) -> Unit) {
        viewModelScope.launch {
            val csv = com.example.utils.BackupHelper.exportToCsv(repository)
            callback(csv)
        }
    }

    fun importAllDataFromCsv(csvString: String, overwrite: Boolean, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = com.example.utils.BackupHelper.importFromCsv(csvString, repository, overwrite)
            callback(result)
        }
    }
}

class ShoppingViewModelFactory(
    private val application: Application,
    private val repository: ShoppingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
