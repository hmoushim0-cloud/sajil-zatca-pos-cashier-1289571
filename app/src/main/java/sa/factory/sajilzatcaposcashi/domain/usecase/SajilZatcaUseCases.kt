package sa.factory.sajilzatcaposcashi.domain.usecase

import sa.factory.sajilzatcaposcashi.domain.repository.SajilZatcaRepository
import sa.factory.sajilzatcaposcashi.model.SajilZatcaItem
import sa.factory.sajilzatcaposcashi.model.SajilZatcaOrder
import sa.factory.sajilzatcaposcashi.model.SajilZatcaTelemetry
import kotlinx.coroutines.flow.Flow

class GetCatalogUseCase(private val repository: SajilZatcaRepository) {
    operator fun invoke(): Flow<List<SajilZatcaItem>> = repository.catalogFlow
}

class ManageCartUseCase(private val repository: SajilZatcaRepository) {
    val cart = repository.activeCart
    fun add(item: SajilZatcaItem) = repository.addToCart(item)
    fun remove(item: SajilZatcaItem) = repository.removeFromCart(item)
    fun clear() = repository.clearCart()
}

class ProcessCheckoutUseCase(private val repository: SajilZatcaRepository) {
    suspend operator fun invoke(paymentMethod: String = "Mada Contactless EMV"): SajilZatcaOrder =
        repository.checkout(paymentMethod)
}

class ObserveHardwareTelemetryUseCase(private val repository: SajilZatcaRepository) {
    operator fun invoke(): Flow<SajilZatcaTelemetry> = repository.telemetryFlow
}
