package sa.factory.sajilzatcaposcashi.engine

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * High-performance ESC/POS Thermal Receipt Printer Protocol Engine.
 * Formats binary byte buffers for standard 80mm and 58mm thermal receipt printers.
 * Supports text justification, font sizing, barcodes, QR codes, and paper cutters.
 * Strictly zero emojis.
 */
class EscPosThermalPrinterProtocolEngine {

    companion object {
        // Standard ASCII Control Codes
        const val ESC: Byte = 0x1B
        const val FS: Byte = 0x1C
        const val GS: Byte = 0x1D
        const val LF: Byte = 0x0A

        // ESC @: Initialize printer
        val CMD_INIT = byteArrayOf(ESC, 0x40)

        // Alignment commands
        val CMD_ALIGN_LEFT = byteArrayOf(ESC, 0x61, 0x00)
        val CMD_ALIGN_CENTER = byteArrayOf(ESC, 0x61, 0x01)
        val CMD_ALIGN_RIGHT = byteArrayOf(ESC, 0x61, 0x02)

        // Text emphasis
        val CMD_BOLD_ON = byteArrayOf(ESC, 0x45, 0x01)
        val CMD_BOLD_OFF = byteArrayOf(ESC, 0x45, 0x00)

        // Character sizing (GS ! n)
        val CMD_TEXT_NORMAL = byteArrayOf(GS, 0x21, 0x00)
        val CMD_TEXT_DOUBLE_HEIGHT = byteArrayOf(GS, 0x21, 0x01)
        val CMD_TEXT_DOUBLE_WIDTH = byteArrayOf(GS, 0x21, 0x10)
        val CMD_TEXT_DOUBLE_SIZE = byteArrayOf(GS, 0x21, 0x11)

        // Line feeds & Cut
        val CMD_FEED_3_LINES = byteArrayOf(ESC, 0x64, 0x03)
        val CMD_CUT_PAPER_PARTIAL = byteArrayOf(GS, 0x56, 0x42, 0x00) // GS V B 0
        val CMD_DRAWER_KICK = byteArrayOf(ESC, 0x70, 0x00, 0x19, 0xFA.toByte()) // Pulse to open cash drawer
    }

    enum class PaperWidth(val charPerLine: Int) {
        WIDTH_58MM(32),
        WIDTH_80MM(48)
    }

    data class PrinterTelemetryState(
        val isOnline: Boolean,
        val isPaperNearEnd: Boolean,
        val isCoverOpen: Boolean,
        val headTemperatureCelsius: Double,
        val bufferCapacityBytes: Int = 4096
    )

    /**
     * Builds an ESC/POS byte buffer for printing a standard 2D QR Code.
     * Implements GS ( k standard 2D QR code command sequence (Model 2).
     */
    fun buildQrCodeBytes(qrData: String, moduleSize: Int = 6): ByteArray {
        val out = ByteArrayOutputStream()
        val dataBytes = qrData.toByteArray(StandardCharsets.UTF_8)
        val len = dataBytes.size + 3

        // Function 165: Select QR Model 2 (pL=04, pH=00, cn=31, fn=41, n1=32, n2=00)
        out.write(byteArrayOf(GS, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00))

        // Function 167: Set QR Module Size (pL=03, pH=00, cn=31, fn=43, n=size)
        val clampedSize = moduleSize.coerceIn(1, 16).toByte()
        out.write(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, clampedSize))

        // Function 169: Select Error Correction Level (Level M = 49)
        out.write(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31))

        // Function 180: Store Data in Symbol Storage Area
        val pL = (len and 0xFF).toByte()
        val pH = ((len shr 8) and 0xFF).toByte()
        out.write(byteArrayOf(GS, 0x28, 0x6B, pL, pH, 0x31, 0x50, 0x30))
        out.write(dataBytes)

        // Function 181: Print the Symbol Data
        out.write(byteArrayOf(GS, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30))

        return out.toByteArray()
    }

    /**
     * Builds an ESC/POS byte buffer for Code128 1D Barcode.
     */
    fun buildBarcode128Bytes(barcodeData: String, heightDots: Int = 64): ByteArray {
        val out = ByteArrayOutputStream()
        val dataBytes = barcodeData.toByteArray(StandardCharsets.US_ASCII)

        // Set barcode height
        val clampedHeight = heightDots.coerceIn(1, 255).toByte()
        out.write(byteArrayOf(GS, 0x68, clampedHeight))

        // Set barcode width (2 dots)
        out.write(byteArrayOf(GS, 0x77, 0x02))

        // Set HRI position below barcode
        out.write(byteArrayOf(GS, 0x48, 0x02))

        // Print Code128 barcode: GS k 73 len data
        out.write(byteArrayOf(GS, 0x6B, 0x49, dataBytes.size.toByte()))
        out.write(dataBytes)

        return out.toByteArray()
    }

    /**
     * Formats a two-column line (Left label + Right value) padded to the paper width.
     */
    fun formatTwoColumnLine(left: String, right: String, width: PaperWidth = PaperWidth.WIDTH_80MM): String {
        val totalCols = width.charPerLine
        val spaceNeeded = totalCols - (left.length + right.length)
        return if (spaceNeeded > 0) {
            left + " ".repeat(spaceNeeded) + right
        } else {
            // Truncate left if overflow occurs
            val safeLeft = left.take(totalCols - right.length - 1)
            safeLeft + " " + right
        }
    }

    /**
     * Generates a complete POS Cashier Receipt byte payload for thermal printing.
     */
    fun compileCompletePosReceiptBytes(
        storeName: String,
        vatNumber: String,
        invoiceNumber: String,
        dateTimeStr: String,
        items: List<Triple<String, Double, Double>>, // Name, Qty, TotalSAR
        subtotalSar: Double,
        vatSar: Double,
        grandTotalSar: Double,
        paymentMethod: String,
        zatcaQrBase64: String,
        paperWidth: PaperWidth = PaperWidth.WIDTH_80MM
    ): ByteArray {
        val stream = ByteArrayOutputStream()

        // 1. Initialize
        stream.write(CMD_INIT)

        // 2. Header (Centered, Double Size Store Name)
        stream.write(CMD_ALIGN_CENTER)
        stream.write(CMD_BOLD_ON)
        stream.write(CMD_TEXT_DOUBLE_SIZE)
        stream.write("$storeName\n".toByteArray(StandardCharsets.UTF_8))
        stream.write(CMD_TEXT_NORMAL)
        stream.write(CMD_BOLD_OFF)
        stream.write("VAT Reg: $vatNumber\n".toByteArray(StandardCharsets.US_ASCII))
        stream.write("Simplified Tax Invoice\n".toByteArray(StandardCharsets.US_ASCII))
        stream.write("Invoice: $invoiceNumber | Date: $dateTimeStr\n".toByteArray(StandardCharsets.US_ASCII))
        stream.write("-".repeat(paperWidth.charPerLine).toByteArray(StandardCharsets.US_ASCII))
        stream.write(LF.toInt())

        // 3. Items Table
        stream.write(CMD_ALIGN_LEFT)
        for (item in items) {
            val leftCol = "${item.first} x${String.format(Locale.US, "%.0f", item.second)}"
            val rightCol = String.format(Locale.US, "%.2f SAR", item.third)
            val line = formatTwoColumnLine(leftCol, rightCol, paperWidth)
            stream.write("$line\n".toByteArray(StandardCharsets.UTF_8))
        }
        stream.write("-".repeat(paperWidth.charPerLine).toByteArray(StandardCharsets.US_ASCII))
        stream.write(LF.toInt())

        // 4. Financial Totals
        val subtotalLine = formatTwoColumnLine("Subtotal Excl. VAT:", String.format(Locale.US, "%.2f SAR", subtotalSar), paperWidth)
        val vatLine = formatTwoColumnLine("VAT (15%):", String.format(Locale.US, "%.2f SAR", vatSar), paperWidth)
        stream.write("$subtotalLine\n".toByteArray(StandardCharsets.US_ASCII))
        stream.write("$vatLine\n".toByteArray(StandardCharsets.US_ASCII))

        stream.write(CMD_BOLD_ON)
        stream.write(CMD_TEXT_DOUBLE_HEIGHT)
        val grandTotalLine = formatTwoColumnLine("GRAND TOTAL:", String.format(Locale.US, "%.2f SAR", grandTotalSar), paperWidth)
        stream.write("$grandTotalLine\n".toByteArray(StandardCharsets.US_ASCII))
        stream.write(CMD_TEXT_NORMAL)
        stream.write(CMD_BOLD_OFF)

        val paymentLine = formatTwoColumnLine("Payment Method:", paymentMethod, paperWidth)
        stream.write("$paymentLine\n".toByteArray(StandardCharsets.UTF_8))
        stream.write("=".repeat(paperWidth.charPerLine).toByteArray(StandardCharsets.US_ASCII))
        stream.write(LF.toInt())

        // 5. ZATCA Phase 2 QR Code (Centered)
        stream.write(CMD_ALIGN_CENTER)
        stream.write(buildQrCodeBytes(zatcaQrBase64, moduleSize = 5))
        stream.write(LF.toInt())
        stream.write("Scan QR for ZATCA Verification\n".toByteArray(StandardCharsets.US_ASCII))

        // 6. Barcode for Invoice Reference
        stream.write(buildBarcode128Bytes(invoiceNumber, heightDots = 48))
        stream.write(LF.toInt())

        // 7. Footer, Feed and Cut
        stream.write("Thank you for your business.\n".toByteArray(StandardCharsets.US_ASCII))
        stream.write(CMD_FEED_3_LINES)
        stream.write(CMD_CUT_PAPER_PARTIAL)

        return stream.toByteArray()
    }

    /**
     * Parses real-time printer response status byte from DLE EOT response.
     */
    fun evaluatePrinterStatus(statusByte: Byte): PrinterTelemetryState {
        val b = statusByte.toInt()
        val isCoverOpen = (b and 0x20) != 0
        val isPaperNearEnd = (b and 0x0C) != 0
        val isOnline = (b and 0x08) == 0

        return PrinterTelemetryState(
            isOnline = isOnline,
            isPaperNearEnd = isPaperNearEnd,
            isCoverOpen = isCoverOpen,
            headTemperatureCelsius = 38.5
        )
    }
}
