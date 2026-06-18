package com.example.studentapplication.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.studentapplication.data.local.StudentEntity
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportUtils {

    // ─── PDF ─────────────────────────────────────────────────────────────────

    fun exportToPdf(context: Context, students: List<StudentEntity>): File {    //File -> return file

        val pdfDocument = PdfDocument()

        // Page setup — A4 size in points (595 x 842)
        val pageWidth  = 595
        val pageHeight = 842
        val margin     = 40f
        val rowHeight  = 36f
        val headerH    = 100f

        // Paint styles
        val titlePaint = Paint().apply {
            color     = Color.parseColor("#1A1A2E")
            textSize  = 22f
            isFakeBoldText = true
        }
        val subPaint = Paint().apply {
            color    = Color.parseColor("#777777")
            textSize = 12f
        }
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#FF9800")
            style = Paint.Style.FILL
        }
        val headerTextPaint = Paint().apply {
            color    = Color.WHITE
            textSize = 13f
            isFakeBoldText = true
        }
        val cellPaint = Paint().apply {
            color    = Color.parseColor("#333333")
            textSize = 12f
        }
        val altRowPaint = Paint().apply {
            color = Color.parseColor("#FFF8F0")
            style = Paint.Style.FILL
        }
        val linePaint = Paint().apply {
            color       = Color.parseColor("#E0E0E0")
            strokeWidth = 1f
        }

        // Column definitions: label + x position + width
        data class Col(val label: String, val x: Float, val width: Float)
        val cols = listOf(
            Col("#",       margin,        30f),
            Col("Name",    margin + 30f,  140f),
            Col("Email",   margin + 170f, 170f),
            Col("Course",  margin + 340f, 110f),
            Col("Phone",   margin + 450f, 105f)
        )

        var pageNumber   = 1
        var studentIndex = 0
        var yPosition: Float

        while (studentIndex < students.size || pageNumber == 1) {

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page     = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas    //canvas -> drawing board
            yPosition = margin

            // ── Title block (first page only) ──────────────────────────────
            if (pageNumber == 1) {
                canvas.drawText("Student List Report", margin, yPosition + 22f, titlePaint)
                yPosition += 30f
                val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                canvas.drawText("Generated: $date  •  Total: ${students.size} students", margin, yPosition + 14f, subPaint)
                yPosition += 30f
                canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
                yPosition += 16f
            }

            // ── Table header row ────────────────────────────────────────────
            canvas.drawRect(margin, yPosition, pageWidth - margin, yPosition + rowHeight, headerBgPaint)
            cols.forEach { col ->
                canvas.drawText(col.label, col.x + 4f, yPosition + 24f, headerTextPaint)
            }
            yPosition += rowHeight

            // ── Student rows ────────────────────────────────────────────────
            val rowsPerPage = ((pageHeight - yPosition - margin) / rowHeight).toInt()
            var rowsDrawn   = 0

            while (studentIndex < students.size && rowsDrawn < rowsPerPage) {
                val student = students[studentIndex]

                // Alternating row background
                if (rowsDrawn % 2 == 1) {
                    canvas.drawRect(margin, yPosition, pageWidth - margin, yPosition + rowHeight, altRowPaint)
                }

                // Row divider
                canvas.drawLine(margin, yPosition + rowHeight, pageWidth - margin, yPosition + rowHeight, linePaint)

                // Cell text — truncate if too long
                val rowData = listOf(
                    (studentIndex + 1).toString(),
                    student.name.take(18),
                    student.email.take(22),
                    student.course.take(14),
                    student.phone.take(13)
                )
                cols.forEachIndexed { i, col ->
                    canvas.drawText(rowData[i], col.x + 4f, yPosition + 24f, cellPaint)
                }

                yPosition    += rowHeight
                studentIndex++
                rowsDrawn++
            }

            pdfDocument.finishPage(page)
            pageNumber++

            // Stop if all students have been drawn
            if (studentIndex >= students.size) break
        }

        // Save to app's external files dir — no permission needed on API 29+
        val fileName = "students_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(null), fileName)
        pdfDocument.writeTo(file.outputStream())
        pdfDocument.close()

        return file
    }

    // ─── CSV ─────────────────────────────────────────────────────────────────

    fun exportToCsv(context: Context, students: List<StudentEntity>): File {

        val fileName = "students_${System.currentTimeMillis()}.csv"
        val file     = File(context.getExternalFilesDir(null), fileName)

        FileWriter(file).use { writer ->
            // Header row
            writer.append("No,Name,Email,Course,Phone\n")

            // Data rows — wrap fields in quotes to handle commas inside values
            students.forEachIndexed { index, student ->
                writer.append("${index + 1},")
                writer.append("\"${student.name.replace("\"", "\"\"")}\",")
                writer.append("\"${student.email.replace("\"", "\"\"")}\",")
                writer.append("\"${student.course.replace("\"", "\"\"")}\",")
                writer.append("\"${student.phone.replace("\"", "\"\"")}\"\n")
            }
        }

        return file
    }
}