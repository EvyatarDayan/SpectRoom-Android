package com.example.spectroom.data.local

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.spectroom.data.model.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BillingHistoryExport {
    private val fileDateFormatter = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
    private val exportedAtFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val invoiceDateFormatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    fun recentInvoices(invoices: List<Invoice>, nowMillis: Long = System.currentTimeMillis()): List<Invoice> {
        val cutoff = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.YEAR, -3)
        }.timeInMillis
        return invoices
            .filter { it.dateMillis >= cutoff }
            .sortedByDescending { it.dateMillis }
    }

    fun writePdfFile(
        context: Context,
        accountEmail: String,
        invoices: List<Invoice>,
        title: String,
        exportedAtTemplate: String,
        accountTemplate: String,
        rangeNote: String,
        emptyLabel: String,
        planWord: String,
        intervalLabel: (Invoice) -> String,
    ): File {
        val lines = buildList {
            add(title)
            add(
                String.format(
                    Locale.getDefault(),
                    exportedAtTemplate,
                    exportedAtFormatter.format(Date()),
                ),
            )
            if (accountEmail.isNotBlank()) {
                add(
                    String.format(
                        Locale.getDefault(),
                        accountTemplate,
                        accountEmail,
                    ),
                )
            }
            add(rangeNote)
            add("")
            if (invoices.isEmpty()) {
                add(emptyLabel)
            } else {
                invoices.forEach { invoice ->
                    add(invoiceDateFormatter.format(Date(invoice.dateMillis)))
                    add(invoice.displayTitle(intervalLabel(invoice), planWord))
                    add(invoice.formattedAmount())
                    add("")
                }
            }
        }

        val file = File(
            context.cacheDir,
            "SpectRoom-Billing-History-${fileDateFormatter.format(Date())}.pdf",
        )
        val document = PdfDocument()
        val pageWidth = 612
        val pageHeight = 792
        val margin = 48f
        val lineHeight = 18f
        val paint = Paint().apply {
            isAntiAlias = true
            textSize = 12f
            color = android.graphics.Color.BLACK
        }
        val titlePaint = Paint(paint).apply {
            textSize = 16f
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

        lines.forEachIndexed { index, line ->
            val useTitle = index == 0
            val p = if (useTitle) titlePaint else paint
            val wrapped = wrapLine(line, p, pageWidth - margin * 2)
            for (part in wrapped) {
                if (y + lineHeight > pageHeight - margin) newPage()
                canvas.drawText(part, margin, y + p.textSize, p)
                y += lineHeight
            }
        }
        document.finishPage(page)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
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
