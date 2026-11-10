package sa.factory.sajilzatcaposcashi.domain.repository

import sa.factory.sajilzatcaposcashi.model.SajilZatcaItem
import sa.factory.sajilzatcaposcashi.model.SajilZatcaOrder
import sa.factory.sajilzatcaposcashi.model.SajilZatcaTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface SajilZatcaRepository {
    val catalogFlow: Flow<List<SajilZatcaItem>>
    val activeCart: StateFlow<List<SajilZatcaItem>>
    val telemetryFlow: Flow<SajilZatcaTelemetry>
    
    fun addToCart(item: SajilZatcaItem)
    fun removeFromCart(item: SajilZatcaItem)
    fun clearCart()
    suspend fun checkout(paymentMethod: String): SajilZatcaOrder
    suspend fun refreshCatalog()
}
