package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ExpenseCategory
import com.example.data.ExpenseEntity
import com.example.network.ExtractedExpense
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val expenses by viewModel.allExpenses.collectAsState()
    val extractorState by viewModel.extractorState.collectAsState()
    val insightsState by viewModel.insightsState.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showScanDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    // Statistics calculations
    val totalExpenditure = expenses.sumOf { it.amount }
    val foodExpenditure = expenses.filter { it.category.equals("Food", true) }.sumOf { it.amount }
    val utilityExpenditure = expenses.filter { it.category.equals("Utility", true) }.sumOf { it.amount }
    val subExpenditure = expenses.filter { it.category.equals("Subscriptions", true) }.sumOf { it.amount }
    val otherExpenditure = expenses.filter { it.category.equals("Others", true) }.sumOf { it.amount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Expense Tracker",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(
                        onClick = { showScanDialog = true },
                        modifier = Modifier.testTag("scan_invoice_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Scan Invoice"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.testTag("add_expense_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense Entry"
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Total Expenditure & Big Summary Cards
            item {
                SummaryHeader(
                    totalAmount = totalExpenditure,
                    transactionCount = expenses.size
                )
            }

            // Section 2: Analytics Donut Chart
            item {
                DonutChartCard(
                    foodSum = foodExpenditure,
                    utilitySum = utilityExpenditure,
                    subSum = subExpenditure,
                    otherSum = otherExpenditure,
                    total = totalExpenditure
                )
            }

            // Section 3: AI Actions Panel (Insights & Presets trigger)
            item {
                InsightsPanelCard(
                    insightsState = insightsState,
                    onGenerateRequest = { viewModel.generateInsights(expenses) }
                )
            }

            // Section 4: Transaction History header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Transaction History",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${expenses.size} total",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            if (expenses.isEmpty()) {
                item {
                    EmptyStateCard(onScanClick = { showScanDialog = true })
                }
            } else {
                items(expenses, key = { it.id }) { expense ->
                    TransactionItemRow(
                        expense = expense,
                        onEditClick = { editingExpense = expense },
                        onDeleteClick = { viewModel.deleteExpense(expense) }
                    )
                }
            }

            // Extra bottom spacer
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Modal: Scan Invoice Dialog
    if (showScanDialog) {
        ScanInvoiceDialog(
            extractorState = extractorState,
            onPresetSelect = { preset -> viewModel.scanPresetInvoice(preset) },
            onSaveParsed = { parsed ->
                viewModel.addExpense(
                    title = parsed.billName,
                    vendor = parsed.vendorName,
                    amount = parsed.totalAmount,
                    category = parsed.category,
                    dateLong = System.currentTimeMillis()
                )
                viewModel.resetExtractorState()
                showScanDialog = false
            },
            onCancel = {
                viewModel.resetExtractorState()
                showScanDialog = false
            }
        )
    }

    // Modal: Add Expense Entry manually
    if (showAddDialog) {
        AddEditExpenseDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, vendor, amount, category, date ->
                viewModel.addExpense(title, vendor, amount, category, date)
                showAddDialog = false
            }
        )
    }

    // Modal: Edit Expense Entry
    editingExpense?.let { expense ->
        AddEditExpenseDialog(
            expenseToEdit = expense,
            onDismiss = { editingExpense = null },
            onSave = { title, vendor, amount, category, date ->
                viewModel.updateExpense(expense.id, title, vendor, amount, category, date)
                editingExpense = null
            }
        )
    }
}

@Composable
fun SummaryHeader(
    totalAmount: Double,
    transactionCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TOTAL EXPENDITURE Summary",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("$%,.2f", totalAmount),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = "$transactionCount Active Statements",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DonutChartCard(
    foodSum: Double,
    utilitySum: Double,
    subSum: Double,
    otherSum: Double,
    total: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Expense Allocation by Category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (total == 0.0) {
                // Return empty instructions block
                Text(
                    text = "No analytics data available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Draw Donut directly
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(140.dp)
                    ) {
                        Canvas(modifier = Modifier.size(130.dp)) {
                            var startAngle = -90f
                            val fAngle = (foodSum / total * 360).toFloat()
                            val uAngle = (utilitySum / total * 360).toFloat()
                            val sAngle = (subSum / total * 360).toFloat()
                            val oAngle = (otherSum / total * 360).toFloat()

                            // Food
                            if (fAngle > 0) {
                                drawArc(
                                    color = Color(android.graphics.Color.parseColor(ExpenseCategory.FOOD.colorHex)),
                                    startAngle = startAngle,
                                    sweepAngle = fAngle,
                                    useCenter = false,
                                    style = Stroke(width = 32f)
                                )
                                startAngle += fAngle
                            }
                            // Utility
                            if (uAngle > 0) {
                                drawArc(
                                    color = Color(android.graphics.Color.parseColor(ExpenseCategory.UTILITY.colorHex)),
                                    startAngle = startAngle,
                                    sweepAngle = uAngle,
                                    useCenter = false,
                                    style = Stroke(width = 32f)
                                )
                                startAngle += uAngle
                            }
                            // Subscriptions
                            if (sAngle > 0) {
                                drawArc(
                                    color = Color(android.graphics.Color.parseColor(ExpenseCategory.SUBSCRIPTIONS.colorHex)),
                                    startAngle = startAngle,
                                    sweepAngle = sAngle,
                                    useCenter = false,
                                    style = Stroke(width = 32f)
                                )
                                startAngle += sAngle
                            }
                            // Others
                            if (oAngle > 0) {
                                drawArc(
                                    color = Color(android.graphics.Color.parseColor(ExpenseCategory.OTHERS.colorHex)),
                                    startAngle = startAngle,
                                    sweepAngle = oAngle,
                                    useCenter = false,
                                    style = Stroke(width = 32f)
                                )
                            }
                        }

                        // Central percentage layout
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "100%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Spread",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Legends and labels
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        LegendItem(ExpenseCategory.FOOD, foodSum, total)
                        LegendItem(ExpenseCategory.UTILITY, utilitySum, total)
                        LegendItem(ExpenseCategory.SUBSCRIPTIONS, subSum, total)
                        LegendItem(ExpenseCategory.OTHERS, otherSum, total)
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(category: ExpenseCategory, amount: Double, total: Double) {
    val pct = if (total > 0.0) (amount / total * 100).toInt() else 0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    Color(android.graphics.Color.parseColor(category.colorHex)),
                    shape = CircleShape
                )
        )
        Text(
            text = "${category.categoryName} ($pct%):",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = String.format("$%.0f", amount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun InsightsPanelCard(
    insightsState: InsightsState,
    onGenerateRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TipsAndUpdates,
                        contentDescription = "Insights Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "AI Portfolio spending insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onGenerateRequest,
                    modifier = Modifier.testTag("generate_insights_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Gen Insights", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (insightsState) {
                is InsightsState.Idle -> {
                    Text(
                        text = "Click 'Gen Insights' to analyze your complete active log and reveal trends, categories, overspending risks, and advice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is InsightsState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Gemini 2.5 is drawing insights...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is InsightsState.Success -> {
                    Text(
                        text = insightsState.insights,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is InsightsState.Error -> {
                    Text(
                        text = "Error: ${insightsState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    expense: ExpenseEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val categoryObj = ExpenseCategory.fromString(expense.category)
    val color = Color(android.graphics.Color.parseColor(categoryObj.colorHex))

    val icon = when (categoryObj) {
        ExpenseCategory.FOOD -> Icons.Default.Restaurant
        ExpenseCategory.UTILITY -> Icons.Default.ElectricBolt
        ExpenseCategory.SUBSCRIPTIONS -> Icons.Default.SmartScreen
        ExpenseCategory.OTHERS -> Icons.Default.ReceiptLong
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(color.copy(alpha = 0.12f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = expense.category,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${expense.vendor}  |  ${getFormattedDate(expense.dateLong)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = String.format("$%.2f", expense.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Black,
                color = color
            )

            IconButton(
                onClick = onEditClick,
                modifier = Modifier.testTag("edit_expense_btn_${expense.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit transaction entry",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_expense_btn_${expense.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete transaction entry",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(onScanClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = "Empty Logo",
                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No recorded transactions yet",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Use mock invoice scanner to simulate uploading bills and receipts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan/Upload Receipt")
            }
        }
    }
}

// Dialog: Scan preset Invoice/Bill Dialog with Gemini parsing confirmation
@Composable
fun ScanInvoiceDialog(
    extractorState: ExtractorState,
    onPresetSelect: (RecipePreset) -> Unit,
    onSaveParsed: (ExtractedExpense) -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "AI Invoice Receipt Scanner",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Upload digital bills, statements or merchant receipts. Select one of the high-fidelity mock thermal presets below to draw on the fly and upload to Gemini 2.5 Flash API for instant OCR and categorization:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                when (extractorState) {
                    is ExtractorState.Idle -> {
                        Text(
                            text = "SELECT PRESET RECEIPT TO SCAN:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        MockInvoiceData.presets.forEach { preset ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onPresetSelect(preset) },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(preset.merchant, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(preset.name, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Text(
                                        String.format("$%,.2f", preset.total),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    is ExtractorState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Running Gemini Multimodal OCR...",
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Extracting details & classifying category.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    is ExtractorState.Success -> {
                        val expense = extractorState.expense
                        val bitmap = extractorState.bitmap

                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Gemini OCR Extracted Data Successful!",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Simulated Thermal Receipt",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            // Extracted summary list
                            ExtractedInfoRow("Merchant:", expense.vendorName)
                            ExtractedInfoRow("Descriptor:", expense.billName)
                            ExtractedInfoRow("Total Amount:", String.format("$%.2f", expense.totalAmount))
                            ExtractedInfoRow("Bill Date:", expense.date)
                            ExtractedInfoRow("Category:", expense.category)

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCancel,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Discard")
                                }
                                Button(
                                    onClick = { onSaveParsed(expense) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save Transaction")
                                }
                            }
                        }
                    }
                    is ExtractorState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = "Error icon",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = extractorState.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (extractorState !is ExtractorState.Success) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun ExtractedInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.bodySmall)
    }
}

// Dialog: Add/Edit Expense entry Dialog (CRUD helper)
@Composable
fun AddEditExpenseDialog(
    expenseToEdit: ExpenseEntity? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, vendor: String, amount: Double, category: String, date: Long) -> Unit
) {
    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var vendor by remember { mutableStateOf(expenseToEdit?.vendor ?: "") }
    var amountText by remember { mutableStateOf(expenseToEdit?.amount?.toString() ?: "") }
    var category by remember { mutableStateOf(expenseToEdit?.category ?: "Food") }

    val categoriesList = listOf("Food", "Utility", "Subscriptions", "Others")
    var isDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (expenseToEdit == null) "Add Expense Statement" else "Edit Expense Statement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title/Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Vendor/Merchant") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_vendor_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Dropdown selector mimicking modern material 3 menu
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { isDropdownExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_category_selector")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Category: $category")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false }
                    ) {
                        categoriesList.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (title.isNotEmpty() && vendor.isNotEmpty() && amt > 0) {
                                onSave(
                                    title,
                                    vendor,
                                    amt,
                                    category,
                                    expenseToEdit?.dateLong ?: System.currentTimeMillis()
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_expense_confirm_button")
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

private fun getFormattedDate(timeMs: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timeMs))
}
