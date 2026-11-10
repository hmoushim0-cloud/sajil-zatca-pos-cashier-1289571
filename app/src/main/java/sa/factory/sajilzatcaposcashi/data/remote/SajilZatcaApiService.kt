package sa.factory.sajilzatcaposcashi.data.remote

import sa.factory.sajilzatcaposcashi.model.SajilZatcaItem
import sa.factory.sajilzatcaposcashi.model.SajilZatcaOrder

interface SajilZatcaApiService {
    suspend fun fetchCatalog(): List<SajilZatcaItem>
    suspend fun submitOrder(order: SajilZatcaOrder): Boolean
}
