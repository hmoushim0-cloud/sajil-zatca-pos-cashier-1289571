package sa.factory.sajilzatcaposcashi.data.repository

import sa.factory.sajilzatcaposcashi.domain.repository.SajilZatcaRepository
import sa.factory.sajilzatcaposcashi.hardware.HardwareSensorDriver
import sa.factory.sajilzatcaposcashi.model.SajilZatcaItem
import sa.factory.sajilzatcaposcashi.model.SajilZatcaOrder
import sa.factory.sajilzatcaposcashi.model.SajilZatcaTelemetry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SajilZatcaRepositoryImpl(
    private val sensorDriver: HardwareSensorDriver
) : SajilZatcaRepository {

    private val _catalog = MutableStateFlow<List<SajilZatcaItem>>(listOf(
        SajilZatcaItem("1", "إعداد بيكسل فيسبوك جديد", "إلغاء البيكسل القديم وربط بيكسل محدث مع كتالوج المنتجات عبر واجهة برمجة تطبيقات شوبيفاي", 150.0, "معتمد", "إعداد البيكسل"),
        SajilZatcaItem("2", "مزامنة كتالوج المنتجات", "تحديث تلقائي للأسعار والمخزون بين متجر شوبيفاي وحساب إعلانات فيسبوك", 280.0, "فوري", "مزامنة الكتالوج"),
        SajilZatcaItem("3", "تكامل طابعة الفواتير", "طباعة فواتير المبيعات عبر الإنترنت مباشرة على طابعات ESC/POS الحرارية مع باركود", 95.0, "جديد", "تقارير المبيعات")
    ))
    override val catalogFlow: Flow<List<SajilZatcaItem>> = _catalog.asStateFlow()

    private val _cart = MutableStateFlow<List<SajilZatcaItem>>(emptyList())
    override val activeCart: StateFlow<List<SajilZatcaItem>> = _cart.asStateFlow()

    override val telemetryFlow: Flow<SajilZatcaTelemetry> = sensorDriver.observePeripheralStream()
        .map { frame ->
            SajilZatcaTelemetry(
                peripheralName = frame.peripheralName,
                status = frame.connectionState,
                liveReading = frame.livePayload,
                batteryPct = 96,
                signalDbm = -54
            )
        }

    override fun addToCart(item: SajilZatcaItem) {
        val current = _cart.value.toMutableList()
        current.add(item)
        _cart.value = current
    }

    override fun removeFromCart(item: SajilZatcaItem) {
        val current = _cart.value.toMutableList()
        current.remove(item)
        _cart.value = current
    }

    override fun clearCart() {
        _cart.value = emptyList()
    }

    override suspend fun checkout(paymentMethod: String): SajilZatcaOrder {
        val currentItems = _cart.value.toList()
        val subtotal = currentItems.sumOf { it.priceSar }
        val vat = subtotal * 0.15
        val total = subtotal + vat
        val order = SajilZatcaOrder(
            orderId = "ORD-${System.currentTimeMillis() % 100000}",
            items = currentItems,
            subtotalSar = subtotal,
            vatSar = vat,
            totalSar = total,
            paymentMethod = paymentMethod
        )
        clearCart()
        return order
    }

    override suspend fun refreshCatalog() {
        // Offline-first sync logic
    }
}
