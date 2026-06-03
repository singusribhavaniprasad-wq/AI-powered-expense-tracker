package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ExpenseDatabase
import com.example.data.ExpenseEntity
import com.example.data.ExpenseRepository
import com.example.network.GeminiApiClient
import com.example.network.ExtractedExpense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

sealed interface ExtractorState {
    object Idle : ExtractorState
    object Loading : ExtractorState
    data class Success(val expense: ExtractedExpense, val bitmap: Bitmap?) : ExtractorState
    data class Error(val message: String) : ExtractorState
}

sealed interface InsightsState {
    object Idle : InsightsState
    object Loading : InsightsState
    data class Success(val insights: String) : InsightsState
    data class Error(val message: String) : InsightsState
}

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ExpenseRepository

    val allExpenses: StateFlow<List<ExpenseEntity>>
    
    private val _extractorState = MutableStateFlow<ExtractorState>(ExtractorState.Idle)
    val extractorState: StateFlow<ExtractorState> = _extractorState.asStateFlow()

    private val _insightsState = MutableStateFlow<InsightsState>(InsightsState.Idle)
    val insightsState: StateFlow<InsightsState> = _insightsState.asStateFlow()

    init {
        val db = ExpenseDatabase.getDatabase(application)
        repository = ExpenseRepository(db.expenseDao())
        
        allExpenses = repository.allExpensesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
        
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = repository.getAllExpenses()
            if (currentList.isEmpty()) {
                // Populate initial mock data for first time boot
                populateInitialData()
            }
        }
    }

    private suspend fun populateInitialData() {
        val initialList = listOf(
            ExpenseEntity(
                title = "Weekly Grocery",
                vendor = "Whole Foods Market",
                amount = 74.50,
                category = "Food",
                dateLong = System.currentTimeMillis() - 86400000L * 2
            ),
            ExpenseEntity(
                title = "VPS Host Subscription",
                vendor = "DigitalOcean Server",
                amount = 24.00,
                category = "Subscriptions",
                dateLong = System.currentTimeMillis() - 86400000L * 4
            ),
            ExpenseEntity(
                title = "Power & Gas October",
                vendor = "PG&E Electric Utility",
                amount = 115.80,
                category = "Utility",
                dateLong = System.currentTimeMillis() - 86400000L * 7
            ),
            ExpenseEntity(
                title = "Leather Jacket Sale",
                vendor = "Zara Outfitters",
                amount = 129.00,
                category = "Others",
                dateLong = System.currentTimeMillis() - 86400000L * 10
            )
        )
        for (expense in initialList) {
            repository.insertExpense(expense)
        }
    }

    // CRUD - Create / Insert
    fun addExpense(title: String, vendor: String, amount: Double, category: String, dateLong: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertExpense(
                ExpenseEntity(
                    title = title,
                    vendor = vendor,
                    amount = amount,
                    category = category,
                    dateLong = dateLong
                )
            )
        }
    }

    // CRUD - Update
    fun updateExpense(id: Long, title: String, vendor: String, amount: Double, category: String, dateLong: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExpense(
                ExpenseEntity(
                    id = id,
                    title = title,
                    vendor = vendor,
                    amount = amount,
                    category = category,
                    dateLong = dateLong
                )
            )
        }
    }

    // CRUD - Delete
    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }

    fun deleteExpenseById(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpenseById(id)
        }
    }

    // AI - OCR Receipt Process
    fun scanPresetInvoice(preset: RecipePreset) {
        _extractorState.value = ExtractorState.Loading
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    MockInvoiceData.createReceiptBitmap(preset)
                }
                val base64 = withContext(Dispatchers.Default) {
                    MockInvoiceData.bitmapToBase64(bitmap)
                }
                val result = withContext(Dispatchers.IO) {
                    GeminiApiClient.extractExpenseFromImage(base64)
                }
                if (result != null) {
                    _extractorState.value = ExtractorState.Success(result, bitmap)
                } else {
                    _extractorState.value = ExtractorState.Error("Extraction failed. Please check Gemini API Key configuration.")
                }
            } catch (e: Exception) {
                _extractorState.value = ExtractorState.Error("Error scanning: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    fun resetExtractorState() {
        _extractorState.value = ExtractorState.Idle
    }

    // AI - Spend Advisor Financial Insights
    fun generateInsights(expenses: List<ExpenseEntity>) {
        if (expenses.isEmpty()) {
            _insightsState.value = InsightsState.Success("No expenses recorded yet to generate insights. Add some expenses first!")
            return
        }

        _insightsState.value = InsightsState.Loading
        viewModelScope.launch {
            try {
                // Build a structured text list for Gemini to analyze
                val logsText = expenses.joinToString("\n") {
                    "- Merchant: ${it.vendor}, Description: ${it.title}, Amount: $${it.amount}, Category: ${it.category}, Date: ${getFormattedDate(it.dateLong)}"
                }
                val result = withContext(Dispatchers.IO) {
                    GeminiApiClient.generateFinancialInsights(logsText)
                }
                if (result != null) {
                    _insightsState.value = InsightsState.Success(result)
                } else {
                    _insightsState.value = InsightsState.Error("Unable to get financial insights. Check API Key setup.")
                }
            } catch (e: Exception) {
                _insightsState.value = InsightsState.Error("Error: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private fun getFormattedDate(timeMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timeMs))
    }
}
