package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mi Carrito de Compra", appName)
  }

  @Test
  fun `test JSON Backup and Restore`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repository = com.example.repository.ShoppingRepository(db.shoppingDao())

    try {
        repository.initializeDefaultTags(force = true)
        val listId = repository.insertList(com.example.data.ShoppingList(name = "Test List")).toInt()
        repository.insertProduct(com.example.data.TemplateProduct(
            listId = listId,
            name = "Milk",
            tag = "Lácteos"
        ))

        val json = com.example.utils.BackupHelper.exportToJson(repository)
        println("EXPORTED JSON:\n$json")

        val importSuccess = com.example.utils.BackupHelper.importFromJson(json, repository, overwrite = true)
        println("IMPORT SUCCESS: $importSuccess")
        assert(importSuccess) { "Import from JSON failed" }

        val lists = repository.getAllListsRaw()
        assertEquals(1, lists.size)
        assertEquals("Test List", lists[0].name)
    } finally {
        db.close()
    }
  }

  @Test
  fun `test CSV Backup and Restore`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repository = com.example.repository.ShoppingRepository(db.shoppingDao())

    try {
        repository.initializeDefaultTags(force = true)
        val listId = repository.insertList(com.example.data.ShoppingList(name = "Test List")).toInt()
        repository.insertProduct(com.example.data.TemplateProduct(
            listId = listId,
            name = "Milk",
            tag = "Lácteos"
        ))

        val csv = com.example.utils.BackupHelper.exportToCsv(repository)
        println("EXPORTED CSV:\n$csv")

        val importSuccess = com.example.utils.BackupHelper.importFromCsv(csv, repository, overwrite = true)
        println("IMPORT SUCCESS SV: $importSuccess")
        assert(importSuccess) { "Import from CSV failed" }

        val lists = repository.getAllListsRaw()
        assertEquals(1, lists.size)
    } finally {
        db.close()
    }
  }
}
