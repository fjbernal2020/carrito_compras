package com.example.utils

import com.example.data.*
import com.example.repository.ShoppingRepository
import org.json.JSONArray
import org.json.JSONObject

object BackupHelper {

    suspend fun exportToJson(repository: ShoppingRepository): String {
        val lists = repository.getAllListsRaw()
        val products = repository.getAllTemplateProductsRaw()
        val historyGroups = repository.getAllHistoryGroupsRaw()
        val historyItems = repository.getAllHistoryItemsRaw()
        val customTags = repository.getAllCustomTagsRaw()

        val rootObj = JSONObject()

        // 1. Lists
        val listsArr = JSONArray()
        for (list in lists) {
            val listObj = JSONObject().apply {
                put("id", list.id)
                put("name", list.name)
                put("createdAt", list.createdAt)
            }
            listsArr.put(listObj)
        }
        rootObj.put("lists", listsArr)

        // 2. Products
        val productsArr = JSONArray()
        for (prod in products) {
            val prodObj = JSONObject().apply {
                put("id", prod.id)
                put("listId", prod.listId)
                put("name", prod.name)
                put("quantity", prod.quantity)
                put("tag", prod.tag)
                put("lastPrice", prod.lastPrice)
                put("isSelectedForTrip", prod.isSelectedForTrip)
            }
            productsArr.put(prodObj)
        }
        rootObj.put("products", productsArr)

        // 3. History Groups
        val groupsArr = JSONArray()
        for (group in historyGroups) {
            val groupObj = JSONObject().apply {
                put("id", group.id)
                put("listName", group.listName)
                put("timestamp", group.timestamp)
                put("totalAmount", group.totalAmount)
            }
            groupsArr.put(groupObj)
        }
        rootObj.put("history_groups", groupsArr)

        // 4. History Items
        val itemsArr = JSONArray()
        for (item in historyItems) {
            val itemObj = JSONObject().apply {
                put("id", item.id)
                put("historyGroupId", item.historyGroupId)
                put("name", item.name)
                put("quantity", item.quantity)
                put("price", item.price)
                put("tag", item.tag)
            }
            itemsArr.put(itemObj)
        }
        rootObj.put("history_items", itemsArr)

        // 5. Custom Tags
        val tagsArr = JSONArray()
        for (tag in customTags) {
            val tagObj = JSONObject().apply {
                put("name", tag.name)
                put("emoji", tag.emoji)
                put("colorLightHex", tag.colorLightHex)
                put("colorDarkHex", tag.colorDarkHex)
            }
            tagsArr.put(tagObj)
        }
        rootObj.put("custom_tags", tagsArr)

        return rootObj.toString(4)
    }

    suspend fun importFromJson(
        jsonString: String,
        repository: ShoppingRepository,
        overwrite: Boolean
    ): Boolean {
        try {
            val cleaned = jsonString.trim().let { if (it.startsWith("\uFEFF")) it.substring(1) else it }.trim()
            val rootObj = JSONObject(cleaned)

            if (overwrite) {
                repository.clearDatabase()
            }

            // Maps to track old to new IDs for auto-generated relation fields
            val listIdMap = mutableMapOf<Int, Int>()
            val historyGroupIdMap = mutableMapOf<Int, Int>()

            // 1. Restore Custom Tags
            if (rootObj.has("custom_tags")) {
                val tagsArr = rootObj.getJSONArray("custom_tags")
                for (i in 0 until tagsArr.length()) {
                    val tagObj = tagsArr.getJSONObject(i)
                    val tag = CustomTag(
                        name = tagObj.getString("name"),
                        emoji = tagObj.optString("emoji", "🏷️"),
                        colorLightHex = tagObj.optString("colorLightHex", "#F5F5F5"),
                        colorDarkHex = tagObj.optString("colorDarkHex", "#303030")
                    )
                    repository.insertCustomTagDirect(tag)
                }
            }

            // Restore default custom tags just in case
            repository.initializeDefaultTags(force = false)

            // 2. Restore Lists
            if (rootObj.has("lists")) {
                val listsArr = rootObj.getJSONArray("lists")
                for (i in 0 until listsArr.length()) {
                    val listObj = listsArr.getJSONObject(i)
                    val oldId = listObj.getInt("id")
                    val name = listObj.getString("name")
                    val createdAt = listObj.optLong("createdAt", System.currentTimeMillis())

                    val newId = repository.insertList(
                        ShoppingList(
                            name = name,
                            createdAt = createdAt
                        )
                    ).toInt()
                    listIdMap[oldId] = newId
                }
            }

            // 3. Restore Products
            if (rootObj.has("products")) {
                val prodsArr = rootObj.getJSONArray("products")
                for (i in 0 until prodsArr.length()) {
                    val prodObj = prodsArr.getJSONObject(i)
                    val oldListId = prodObj.getInt("listId")
                    val newListId = listIdMap[oldListId] ?: continue // Skip if list doesn't exist now

                    val prod = TemplateProduct(
                        listId = newListId,
                        name = prodObj.getString("name"),
                        quantity = prodObj.optInt("quantity", 1),
                        tag = prodObj.optString("tag", "Otros"),
                        lastPrice = prodObj.optDouble("lastPrice", 0.0),
                        isSelectedForTrip = prodObj.optBoolean("isSelectedForTrip", true)
                    )
                    repository.insertProduct(prod)
                }
            }

            // 4. Restore History Groups
            if (rootObj.has("history_groups")) {
                val groupsArr = rootObj.getJSONArray("history_groups")
                for (i in 0 until groupsArr.length()) {
                    val groupObj = groupsArr.getJSONObject(i)
                    val oldId = groupObj.getInt("id")
                    val listName = groupObj.getString("listName")
                    val timestamp = groupObj.optLong("timestamp", System.currentTimeMillis())
                    val totalAmount = groupObj.getDouble("totalAmount")

                    val newId = repository.insertHistoryGroup(
                        PurchaseHistoryGroup(
                            listName = listName,
                            timestamp = timestamp,
                            totalAmount = totalAmount
                        )
                    ).toInt()
                    historyGroupIdMap[oldId] = newId
                }
            }

            // 5. Restore History Items
            if (rootObj.has("history_items")) {
                val itemsArr = rootObj.getJSONArray("history_items")
                val listToInsert = mutableListOf<PurchaseHistoryItem>()
                for (i in 0 until itemsArr.length()) {
                    val itemObj = itemsArr.getJSONObject(i)
                    val oldGroupId = itemObj.getInt("historyGroupId")
                    val newGroupId = historyGroupIdMap[oldGroupId] ?: continue

                    val item = PurchaseHistoryItem(
                        historyGroupId = newGroupId,
                        name = itemObj.getString("name"),
                        quantity = itemObj.getInt("quantity"),
                        price = itemObj.getDouble("price"),
                        tag = itemObj.optString("tag", "Otros")
                    )
                    listToInsert.add(item)
                }
                if (listToInsert.isNotEmpty()) {
                    repository.insertHistoryItems(listToInsert)
                }
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    // --- CSV Format Utilities ---
    // Exports only Lists & TemplateProducts in CSV format
    suspend fun exportToCsv(repository: ShoppingRepository): String {
        val lists = repository.getAllListsRaw()
        val products = repository.getAllTemplateProductsRaw()
        val listsMap = lists.associateBy { it.id }

        val sb = java.lang.StringBuilder()
        // Header
        sb.append("Lista,Producto,Cantidad,Precio,Categoria,SeleccionadoParaIrAComprar\n")

        for (prod in products) {
            val listName = listsMap[prod.listId]?.name ?: "Desconocido"
            // Escape values containing commas or quotes
            val escapedListName = csvEscape(listName)
            val escapedProdName = csvEscape(prod.name)
            val escapedTag = csvEscape(prod.tag)

            sb.append("$escapedListName,$escapedProdName,${prod.quantity},${prod.lastPrice},$escapedTag,${prod.isSelectedForTrip}\n")
        }
        return sb.toString()
    }

    // Imports from CSV format
    suspend fun importFromCsv(
        csvString: String,
        repository: ShoppingRepository,
        overwrite: Boolean
    ): Boolean {
        try {
            val cleaned = csvString.trim().let { if (it.startsWith("\uFEFF")) it.substring(1) else it }.trim()
            if (overwrite) {
                repository.clearDatabase()
                repository.initializeDefaultTags(force = true)
            }

            val lines = cleaned.split("\n")
            if (lines.isEmpty()) return false

            // Track existing list names or created lists to avoid duplicates when merging
            val listsInDb = repository.getAllListsRaw().toMutableList()
            val listNameToId = listsInDb.associate { it.name.lowercase().trim() to it.id }.toMutableMap()

            // Header skip detection: check if first line corresponds to headers
            val startLineIndex = if (lines[0].contains("Lista") && lines[0].contains("Producto")) 1 else 0

            for (i in startLineIndex until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue

                val parts = parseCsvLine(line)
                if (parts.size < 2) continue // Need at least List and Product Name

                val listName = parts[0].trim()
                val prodName = parts[1].trim()
                if (listName.isEmpty() || prodName.isEmpty()) continue

                val qty = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 1
                val rPrice = parts.getOrNull(3)?.trim()?.toDoubleOrNull() ?: 0.0
                val tag = parts.getOrNull(4)?.trim() ?: "Otros"
                val isSelStr = parts.getOrNull(5)?.trim() ?: "true"
                val isSelected = isSelStr.lowercase() == "true" || isSelStr == "1"

                // Get or create List
                val normalizedListName = listName.lowercase().trim()
                var listId = listNameToId[normalizedListName]
                if (listId == null) {
                    val newListId = repository.insertList(ShoppingList(name = listName)).toInt()
                    listId = newListId
                    listNameToId[normalizedListName] = newListId
                }

                // Insert Product
                repository.insertProduct(
                    TemplateProduct(
                        listId = listId,
                        name = prodName,
                        quantity = qty,
                        tag = tag,
                        lastPrice = rPrice,
                        isSelectedForTrip = isSelected
                    )
                )
            }
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    private fun csvEscape(value: String): String {
        var result = value.replace("\"", "\"\"")
        if (result.contains(",") || result.contains("\n") || result.contains("\"")) {
            result = "\"$result\""
        }
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = java.lang.StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length && line[i + 1] == '"') {
                        curVal.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = java.lang.StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }
}
