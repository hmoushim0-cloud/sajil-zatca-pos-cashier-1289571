# سقر للربط الذكي — مزامنة فورية بين شوبيفاي وإعلانات فيسبوك (`sajil-zatca-pos-cashier-1289571`)
> ربط سلس مع طابعات ESC/POS وماسحات الباركود لتعزيز مبيعاتك عبر الإنترنت

- **Project ID**: `#1289571`
- **Commercial Archetype**: `pos_cashier`
- **Bound Hardware Peripheral**: `ESC/POS Thermal Receipt Printer + Barcode Scanner`
- **Package Name**: `sa.factory.sajilzatcaposcashi`
- **Zero-Emoji Invariant**: Strictly Enforced (100% Vector Icons)

---

## [Sidekick Autonomous Update] - Iteration 79

### Production Engines Implemented
1. **ZATCA Phase 2 Cryptographic E-Invoicing Engine (`ZatcaPhase2CryptographicInvoiceEngine.kt`)**:
   - Implements full Tag-Length-Value (TLV) binary encoding for ZATCA Phase 2 QR codes.
   - Generates deterministic SHA-256 invoice hashes and chains previous invoice hash (PIH) with invoice counter values (ICV).
   - Validates Saudi 15-digit VAT registration numbers starting and ending with 3.
   - Supports Standard Tax Invoices (`0100000`), Simplified Invoices (`0200000`), Credit Notes, and Debit Notes.

2. **ESC/POS Thermal Printer Protocol Engine (`EscPosThermalPrinterProtocolEngine.kt`)**:
   - Generates binary protocol streams for standard 80mm and 58mm thermal receipt printers.
   - Encodes text justifications, double-height/width typography, and tabular alignment.
   - Implements GS ( k standard 2D QR Code Model 2 generation and Code128 1D barcode buffers.
   - Controls cash drawer pulse kick and partial paper cut commands (`GS V 66 0`).
   - Decodes real-time printer telemetry (paper status, cover state, thermal head temperatures).

3. **Comprehensive Regression Test Suite (`SajilZatcaRegressionTest.kt`)**:
   - 10 unit test cases verifying VAT rules, TLV byte buffers, invoice hash determinism, ESC/POS byte sequences, cart calculations with 15% VAT, and strict zero-emoji compliance.

---

### Build & Verification Commands
```bash
# Run unit tests
./gradlew test

# Assemble debug APK
./gradlew assembleDebug
```
