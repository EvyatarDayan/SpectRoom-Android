package com.example.spectroom.data.local

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.spectroom.data.model.MessageDisplay
import com.example.spectroom.data.model.MessageType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MessagesExportConversation(
    val partnerName: String,
    val messages: List<MessageDisplay>,
)

object MessagesExport {
    private val fileDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val exportedAt = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val dayHeader = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
    private val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
    private val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun writePdfFile(
        context: Context,
        currentUserName: String,
        currentUserId: String,
        conversations: List<MessagesExportConversation>,
        youLabel: String,
        title: String,
        exportedAtTemplate: String,
        accountTemplate: String,
        conversationWithTemplate: String,
        emptyLabel: String,
        voiceLabel: String,
        photoLabel: String,
        videoLabel: String,
    ): File {
        val lines = buildList {
            add(title)
            add(String.format(Locale.getDefault(), exportedAtTemplate, exportedAt.format(Date())))
            add(String.format(Locale.getDefault(), accountTemplate, currentUserName))
            add("")
            if (conversations.isEmpty()) {
                add(emptyLabel)
            } else {
                conversations.forEachIndexed { index, conversation ->
                    if (index > 0) add("")
                    add("────────────────────────────")
                    add(
                        String.format(
                            Locale.getDefault(),
                            conversationWithTemplate,
                            conversation.partnerName,
                        ),
                    )
                    add("────────────────────────────")
                    add("")
                    var lastDay: String? = null
                    for (message in conversation.messages) {
                        val day = dayKey.format(message.createdAt)
                        if (day != lastDay) {
                            if (lastDay != null) add("")
                            add("--- ${dayHeader.format(message.createdAt)} ---")
                            add("")
                            lastDay = day
                        }
                        val outgoing = message.senderId == currentUserId
                        val sender = if (outgoing) youLabel else message.senderName
                        add("[${timeFmt.format(message.createdAt)}] $sender")
                        add("• ${bodyText(message, voiceLabel, photoLabel, videoLabel)}")
                        add("")
                    }
                }
            }
        }

        val file = File(
            context.cacheDir,
            "SpectRoom-Messages-${fileDate.format(Date())}.pdf",
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

    private fun bodyText(
        message: MessageDisplay,
        voiceLabel: String,
        photoLabel: String,
        videoLabel: String,
    ): String = when (message.messageType) {
        MessageType.VOICE -> voiceLabel
        MessageType.IMAGE -> photoLabel
        MessageType.VIDEO -> videoLabel
        else -> message.content
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
