package com.giyeok.passzero.ui

import java.awt.Color
import java.awt.print.PrinterException
import java.awt.print.PrinterJob
import java.io.File
import java.io.IOException
import scala.util.Random
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.printing.PDFPageable

object PdfBoxPractice {

    def main(args: Array[String]): Unit = {
        val document: PDDocument = new PDDocument()

        (0 until 1) foreach { _ =>
            val page = new PDPage()

            val contentStream = new PDPageContentStream(document, page)

            contentStream.beginText()
            contentStream.setFont(PDType1Font.TIMES_ROMAN, 12)
            contentStream.newLineAtOffset(25, 500)
            contentStream.showText("Hello?")
            contentStream.endText()

            contentStream.beginText()
            contentStream.setFont(PDType1Font.TIMES_BOLD, 20)
            contentStream.newLineAtOffset(25, 600)
            contentStream.setLeading(14.5f)
            contentStream.showText("Line1")
            contentStream.newLine()
            contentStream.showText("Line2")
            contentStream.endText()

            contentStream.setNonStrokingColor(Color.DARK_GRAY)
            (0 until 7) foreach { y =>
                (0 until 7) foreach { x =>
                    if (Random.nextBoolean()) {
                        contentStream.addRect(50 + 10 * x, 50 + 10 * y, 10, 10)
                        contentStream.fill()
                    }
                }
            }

            contentStream.close()

            document.addPage(page)
        }

        document.save(new File("./pdfboxpractice.pdf"))

        try {
            val printerJob = PrinterJob.getPrinterJob
            if (printerJob.printDialog()) {
                printerJob.getPrintService
            }
            printerJob.setPageable(new PDFPageable(document))
            printerJob.print()
        } catch {
            case _: IOException =>
                ???
            case _: PrinterException =>
                ???
        }

        document.close()
    }
}
