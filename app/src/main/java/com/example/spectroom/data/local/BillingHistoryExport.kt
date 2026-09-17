package com.example.spectroom.data.local

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.spectroom.R
import com.example.spectroom.data.model.AccountHistoryEvent
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BillingHistoryExport {
    private val fileDateFormatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    private val exportedAtFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    fun recentEvents(
        events: List<AccountHistoryEvent>,
        nowMillis: Long = System.currentTimeMillis(),
    ): List<AccountHistoryEvent> {
        val cutoff = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.YEAR, -3)
        }.timeInMillis
        return events
            .filter { it.dateMillis >= cutoff }
            .sortedByDescending { it.dateMillis }
    }

    fun writePdfFile(
        context: Context,
        accountEmail: String,
        events: List<AccountHistoryEvent>,
    ): File {
        val title = context.getString(R.string.billing_invoices_export_title)
        val exportedAt = String.format(
            Locale.getDefault(),
            context.getString(R.string.billing_invoices_export_exported_at),
            exportedAtFormatter.format(Date()),
        )
        val accountLine = accountEmail.takeIf { it.isNotBlank() }?.let {
            String.format(
                Locale.getDefault(),
                context.getString(R.string.billing_invoices_export_account),
                it,
            )
        }
        val rangeNote = context.getString(R.string.billing_invoices_export_range)
        val emptyLabel = context.getString(R.string.billing_invoices_empty)

        val file = File(
            context.cacheDir,
            "SpectRoom-Account-History-${fileDateFormatter.format(Date())}.pdf",
        )
        val document = PdfDocument()
        val pageWidth = 612
        val pageHeight = 792
        val margin = 48f
        val amountColumnWidth = 100f
        val leftColumnWidth = pageWidth - margin * 2 - amountColumnWidth - 12f
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            color = android.graphics.Color.BLACK
        }
        val secondaryPaint = Paint(bodyPaint).apply {
            color = android.graphics.Color.DKGRAY
        }
        val boldPaint = Paint(bodyPaint).apply {
            isFakeBoldText = true
        }
        val titlePaint = Paint(bodyPaint).apply {
            textSize = 18f
            isFakeBoldText = true
        }

        var pageNumber = 1
        var y = margin
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create(),
        )
        var canvas = page.canvas

        fun newPage() {
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create(),
            )
            canvas = page.canvas
            y = margin
        }

        fun ensureSpace(needed: Float) {
            if (y + needed > pageHeight - margin) newPage()
        }

        fun drawWrapped(text: String, paint: Paint, x: Float, maxWidth: Float): Float {
            val lines = wrapLine(text, paint, maxWidth)
            var height = 0f
            for (part in lines) {
                canvas.drawText(part, x, y + paint.textSize + height, paint)
                height += paint.textSize + 2f
            }
            return height.coerceAtLeast(paint.textSize)
        }

        ensureSpace(40f)
        y += drawWrapped(title, titlePaint, margin, pageWidth - margin * 2) + 8f
        y += drawWrapped(exportedAt, secondaryPaint, margin, pageWidth - margin * 2) + 4f
        if (accountLine != null) {
            y += drawWrapped(accountLine, secondaryPaint, margin, pageWidth - margin * 2) + 4f
        }
        y += drawWrapped(rangeNote, secondaryPaint, margin, pageWidth - margin * 2) + 16f

        if (events.isEmpty()) {
            drawWrapped(emptyLabel, bodyPaint, margin, pageWidth - margin * 2)
        } else {
            events.forEach { event ->
                val dateText = event.formattedDateTime()
                val titleText = event.displayTitle(context)
                val amountText = if (event.showsAmount) event.formattedAmount() else ""
                val dateHeight = measureHeight(dateText, secondaryPaint, leftColumnWidth)
                val titleHeight = measureHeight(titleText, boldPaint, leftColumnWidth)
                val amountHeight = if (amountText.isEmpty()) {
                    0f
                } else {
                    measureHeight(amountText, boldPaint, amountColumnWidth)
                }
                val rowHeight = maxOf(dateHeight + 2f + titleHeight, amountHeight) + 14f
                ensureSpace(rowHeight)

                val rowTop = y
                y += drawWrapped(dateText, secondaryPaint, margin, leftColumnWidth) + 2f
                drawWrapped(titleText, boldPaint, margin, leftColumnWidth)
                if (amountText.isNotEmpty()) {
                    val amountY = rowTop
                    val previousY = y
                    y = amountY
                    // Right-align by measuring.
                    val amountWidth = boldPaint.measureText(amountText)
                    canvas.drawText(
                        amountText,
                        pageWidth - margin - amountWidth,
                        y + boldPaint.textSize,
                        boldPaint,
                    )
                    y = previousY
                }
                y = rowTop + rowHeight - 8f
                val separatorY = y + 4f
                canvas.drawLine(
                    margin,
                    separatorY,
                    pageWidth - margin,
                    separatorY,
                    Paint().apply {
                        color = android.graphics.Color.LTGRAY
                        strokeWidth = 0.5f
                    },
                )
                y = separatorY + 10f
            }
        }

        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun measureHeight(text: String, paint: Paint, maxWidth: Float): Float {
        val lines = wrapLine(text, paint, maxWidth)
        return lines.size * (paint.textSize + 2f)
    }

    private fun wrapLine(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isEmpty()) return listOf("")
        val words = text.split(' ')
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth) {
                current = StringBuilder(candidate)
            } else {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf("") }
    }
}
