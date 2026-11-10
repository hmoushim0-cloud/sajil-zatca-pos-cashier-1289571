package sa.factory.sajilzatcaposcashi

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.sajilzatcaposcashi.data.repository.SajilZatcaRepositoryImpl
import sa.factory.sajilzatcaposcashi.engine.EscPosThermalPrinterProtocolEngine
import sa.factory.sajilzatcaposcashi.engine.EscPosThermalPrinterProtocolEngine.*
import sa.factory.sajilzatcaposcashi.engine.ZatcaPhase2CryptographicInvoiceEngine
import sa.factory.sajilzatcaposcashi.engine.ZatcaPhase2CryptographicInvoiceEngine.*
import sa.factory.sajilzatcaposcashi.hardware.HardwareSensorDriver
import sa.factory.sajilzatcaposcashi.model.SajilZatcaItem
import java.util.Base64
import java.util.Date

class SajilZatcaRegressionTest {

    private val zatcaEngine = ZatcaPhase2CryptographicInvoiceEngine()
    private val escPosEngine = EscPosThermalPrinterProtocolEngine()

    @Test
    fun testVatNumberValidationRules() {
        // Valid 15-digit Saudi VAT starting and ending with 3
        assertTrue(zatcaEngine.validateVatNumber("310123456789013"))
        assertTrue(zatcaEngine.validateVatNumber("300000000000003"))

        // Invalid cases: wrong start/end, wrong length, non-digits
        assertFalse(zatcaEngine.validateVatNumber("110123456789013")) // Starts with 1
        assertFalse(zatcaEngine.validateVatNumber("310123456789014")) // Ends with 4
        assertFalse(zatcaEngine.validateVatNumber("31012345678901"))  // 14 digits
        assertFalse(zatcaEngine.validateVatNumber("3101234567890134")) // 16 digits
        assertFalse(zatcaEngine.validateVatNumber("31012345678901A")) // Contains letter
    }

    @Test
    fun testTlvTagBinaryEncoding() {
        val testData = "SAJIL".toByteArray(Charsets.UTF_8)
        val tlvBytes = zatcaEngine.encodeTlvTag(1, testData)

        // Tag 1 (1 byte), Length 5 (1 byte), Value 5 bytes = 7 bytes total
        assertEquals(7, tlvBytes.size)
        assertEquals(1.toByte(), tlvBytes[0])
        assertEquals(5.toByte(), tlvBytes[1])
        assertArrayEquals(testData, tlvBytes.sliceArray(2..6))
    }

    @Test
    fun testZatcaPhase2QrTlvBase64Generation() {
        val hash = ByteArray(32) { 0xAA.toByte() }
        val qrBase64 = zatcaEngine.buildPhase2QrTlvBase64(
            sellerName = "شركة سجل للحلول الرقمية",
            vatRegistrationNumber = "310123456789013",
            timestampIsoUtc = "2026-11-10T22:00:00Z",
            invoiceTotalSar = 115.00,
            vatTotalSar = 15.00,
            invoiceHashSha256 = hash
        )

        assertNotNull(qrBase64)
        assertTrue(qrBase64.isNotEmpty())

        val decoded = Base64.getDecoder().decode(qrBase64)
        assertTrue(decoded.isNotEmpty())
        assertEquals(1.toByte(), decoded[0]) // First tag should be tag 1
    }

    @Test
    fun testSha256InvoiceHashingDeterminism() {
        val hash1 = zatcaEngine.computeInvoiceHashSha256(
            invoiceNumber = "INV-2026-001",
            uuid = "a1b2c3d4-e5f6-7890",
            timestampUtc = "2026-11-10T22:00:00Z",
            grandTotalSar = 575.0,
            vatTotalSar = 75.0,
            previousInvoiceHash = "NWZlY2JmOTM2...",
            icv = 42L
        )

        val hash2 = zatcaEngine.computeInvoiceHashSha256(
            invoiceNumber = "INV-2026-001",
            uuid = "a1b2c3d4-e5f6-7890",
            timestampUtc = "2026-11-10T22:00:00Z",
            grandTotalSar = 575.0,
            vatTotalSar = 75.0,
            previousInvoiceHash = "NWZlY2JmOTM2...",
            icv = 42L
        )

        assertEquals(32, hash1.size)
        assertArrayEquals(hash1, hash2)
    }

    @Test
    fun testInvoiceDocumentComplianceCheck() {
        val seller = InvoiceParty(
            name = "متجر سجل التجاري",
            vatNumber = "310123456789013",
            streetAddress = "طريق الملك فهد",
            city = "الرياض",
            postalCode = "12214"
        )
        val items = listOf(
            InvoiceItem("ITM-1", "قارئ باركود ليزري", 2.0, 150.0),
            InvoiceItem("ITM-2", "ورق حراري 80 ملم", 10.0, 12.0)
        )
        val doc = ZatcaInvoiceDocument(
            invoiceNumber = "INV-2026-1001",
            uuid = "550e8400-e29b-41d4-a716-446655440000",
            issueDate = Date(),
            invoiceType = InvoiceType.SIMPLIFIED_TAX_INVOICE,
            seller = seller,
            items = items,
            previousInvoiceHash = "PREV_HASH_BASE64_STAMP",
            invoiceCounterValue = 101L
        )

        val result = zatcaEngine.processAndVerifyInvoice(doc)
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertTrue(result.invoiceHashBase64.isNotEmpty())
        assertTrue(result.qrCodeTlvBase64.isNotEmpty())
    }

    @Test
    fun testEscPosInitializationAndAlignmentBytes() {
        assertArrayEquals(byteArrayOf(0x1B, 0x40), EscPosThermalPrinterProtocolEngine.CMD_INIT)
        assertArrayEquals(byteArrayOf(0x1B, 0x61, 0x00), EscPosThermalPrinterProtocolEngine.CMD_ALIGN_LEFT)
        assertArrayEquals(byteArrayOf(0x1B, 0x61, 0x01), EscPosThermalPrinterProtocolEngine.CMD_ALIGN_CENTER)
        assertArrayEquals(byteArrayOf(0x1B, 0x61, 0x02), EscPosThermalPrinterProtocolEngine.CMD_ALIGN_RIGHT)

        val col = escPosEngine.formatTwoColumnLine("Item A", "50.00 SAR", PaperWidth.WIDTH_80MM)
        assertEquals(PaperWidth.WIDTH_80MM.charPerLine, col.length)
        assertTrue(col.startsWith("Item A"))
        assertTrue(col.endsWith("50.00 SAR"))
    }

    @Test
    fun testEscPosQrCodeCommandStructure() {
        val qrBytes = escPosEngine.buildQrCodeBytes("https://zatca.gov.sa/verify?id=99", moduleSize = 6)
        assertTrue(qrBytes.isNotEmpty())

        // Must start with GS ( k header
        assertEquals(0x1D.toByte(), qrBytes[0])
        assertEquals(0x28.toByte(), qrBytes[1])
        assertEquals(0x6B.toByte(), qrBytes[2])
    }

    @Test
    fun testEscPosReceiptByteCompilation() {
        val items = listOf(
            Triple("قارئ باركود", 1.0, 200.0),
            Triple("ورق حراري", 5.0, 50.0)
        )
        val receipt = escPosEngine.compileCompletePosReceiptBytes(
            storeName = "سجل لحلول نقاط البيع",
            vatNumber = "310123456789013",
            invoiceNumber = "INV-009",
            dateTimeStr = "2026-11-10 22:30",
            items = items,
            subtotalSar = 250.0,
            vatSar = 37.5,
            grandTotalSar = 287.5,
            paymentMethod = "مدى Mada NFC",
            zatcaQrBase64 = "AQZTYWppbA=="
        )

        assertTrue(receipt.isNotEmpty())
        // Ends with cut paper command
        val cutCmd = EscPosThermalPrinterProtocolEngine.CMD_CUT_PAPER_PARTIAL
        val endingBytes = receipt.takeLast(cutCmd.size).toByteArray()
        assertArrayEquals(cutCmd, endingBytes)
    }

    @Test
    fun testCartAndRepositoryCheckoutIntegration() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = SajilZatcaRepositoryImpl(sensorDriver)

        val item1 = SajilZatcaItem("P1", "طابعة إيصالات حرارية 80 ملم", "طابعة متوافقة مع أنظمة الكاشير", 450.0, "معتمد", "أجهزة")
        val item2 = SajilZatcaItem("P2", "ماسح باركود لاسلكي 2D", "ماسح ضوئي عالي السرعة", 250.0, "معتمد", "أجهزة")

        repository.addToCart(item1)
        repository.addToCart(item2)
        assertEquals(2, repository.activeCart.value.size)

        val order = repository.checkout("مدى Mada Contactless")

        assertEquals(700.0, order.subtotalSar, 0.01)
        assertEquals(105.0, order.vatSar, 0.01)
        assertEquals(805.0, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)

        val telemetry = sensorDriver.observePeripheralStream().first()
        assertNotNull(telemetry.peripheralName)
    }

    @Test
    fun testZeroEmojiComplianceAcrossEntities() {
        val regexEmoji = "[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+".toRegex()

        for (type in InvoiceType.values()) {
            assertFalse("Invoice type description must not contain emoji: ${type.descriptionAr}", regexEmoji.containsMatchIn(type.descriptionAr))
        }

        val sampleReceiptText = "سجل لحلول نقاط البيع Simplified Tax Invoice VAT Reg 310123456789013"
        assertFalse("Sample receipt text must not contain emoji", regexEmoji.containsMatchIn(sampleReceiptText))
    }
}
