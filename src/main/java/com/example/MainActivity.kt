package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.repository.ShoppingRepository
import com.example.ui.ShoppingApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ShoppingViewModel
import com.example.viewmodel.ShoppingViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize DB and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ShoppingRepository(database.shoppingDao())

        // 2. Initialize ViewModel with custom factory
        val factory = ShoppingViewModelFactory(application, repository)
        val viewModel = ViewModelProvider(this, factory)[ShoppingViewModel::class.java]

        // 3. Render Compose App
        setContent {
            MyApplicationTheme {
                ShoppingApp(viewModel = viewModel)
            }
        }
    }
}
