package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.io.ByteArrayOutputStream
import android.util.Base64

data class RecipePreset(
    val name: String,
    val merchant: String,
    val date: String,
    val items: List<String>,
    val prices: List<Double>,
    val total: Double,
    val category: String
)

object MockInvoiceData {
    val presets = listOf(
        RecipePreset(
            name = "Coffee break & snacks",
            merchant = "Starbucks Store #982",
            date = "2026-05-24",
            items = listOf("Caramel Macchiato", "Butter Croissant", "Avocado Toast"),
            prices = listOf(5.85, 3.50, 6.25),
            total = 15.60,
            category = "Food"
        ),
        RecipePreset(
            name = "SaaS Server Infrastructure",
            merchant = "Amazon Web Services",
            date = "2026-06-01",
            items = listOf("EC2 Compute Instance", "S3 Storage Pack", "Route53 Domains"),
            prices = listOf(45.20, 12.80, 4.00),
            total = 62.00,
            category = "Subscriptions"
        ),
        RecipePreset(
            name = "Monthly Highspeed Internet",
            merchant = "Comcast Xfinity Premium",
            date = "2026-05-18",
            items = listOf("1Gbps Cable Internet", "Regional Router Lease"),
            prices = listOf(84.99, 15.00),
            total = 99.99,
            category = "Utility"
        ),
        RecipePreset(
            name = "Office chairs & desk kit",
            merchant = "Target Corporation",
            date = "2026-05-29",
            items = listOf("Ergonomic Study Chair", "LED Desk Lamp"),
            prices = listOf(110.00, 24.50),
            total = 134.50,
            category = "Others"
        )
    )

    fun createReceiptBitmap(preset: RecipePreset): Bitmap {
        val width = 450
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw background thermal paper texture
        val bgPaint = Paint().apply {
            color = Color.parseColor("#FBFBFA")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Draw border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(3f, 3f, (width - 3).toFloat(), (height - 3).toFloat(), borderPaint)

        // Title merchant paint
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        // Subtitle paint
        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        // Bold text paint
        val boldPaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }

        var y = 60f
        canvas.drawText(preset.merchant.uppercase(), (width / 2).toFloat(), y, titlePaint)
        y += 35f

        canvas.drawText("Tax Invoice & Billing Statement", (width / 2).toFloat(), y, Paint().apply {
            color = Color.GRAY
            textSize = 16f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        })
        y += 45f

        canvas.drawText("Date: ${preset.date}", 40f, y, textPaint)
        canvas.drawText("No: INV-2026-${(1000..9999).random()}", (width - 180).toFloat(), y, textPaint)
        y += 35f

        // Draw dashed separator line
        val dashPaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(40f, y, (width - 40).toFloat(), y, dashPaint)
        y += 30f

        // Draw items header
        canvas.drawText("ITEM DESCRIPTION", 40f, y, boldPaint)
        canvas.drawText("PRICE", (width - 110).toFloat(), y, boldPaint)
        y += 25f
        canvas.drawLine(40f, y, (width - 40).toFloat(), y, dashPaint)
        y += 30f

        // Draw items
        for (i in preset.items.indices) {
            val item = preset.items[i]
            val price = preset.prices[i]
            val truncatedItem = if (item.length > 22) item.substring(0, 20) + ".." else item
            canvas.drawText(truncatedItem, 40f, y, textPaint)
            canvas.drawText(String.format("$%.2f", price), (width - 110).toFloat(), y, textPaint)
            y += 30f
        }

        y += 10f
        canvas.drawLine(40f, y, (width - 40).toFloat(), y, dashPaint)
        y += 35f

        // Draw Subtotal & Tax
        canvas.drawText("Subtotal:", 200f, y, textPaint)
        canvas.drawText(String.format("$%.2f", preset.total * 0.92), (width - 110).toFloat(), y, textPaint)
        y += 25f
        canvas.drawText("Tax (8%):", 200f, y, textPaint)
        canvas.drawText(String.format("$%.2f", preset.total * 0.08), (width - 110).toFloat(), y, textPaint)
        y += 35f

        canvas.drawLine(200f, y, (width - 40).toFloat(), y, dashPaint)
        y += 30f

        // Total
        canvas.drawText("TOTAL AMOUNT:", 100f, y, boldPaint)
        canvas.drawText(String.format("$%.2f", preset.total), (width - 110).toFloat(), y, boldPaint)
        y += 40f

        // Footer block
        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 14f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("THANK YOU FOR YOUR BUSINESS", (width / 2).toFloat(), y, footerPaint)
        y += 20f
        canvas.drawText("CLASSIFICATION: ${preset.category.uppercase()}", (width / 2).toFloat(), y, footerPaint)

        return bitmap
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
