package sa.factory.sajilzatcaposcashi.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import sa.factory.sajilzatcaposcashi.model.SajilZatcaItem

@Entity(tableName = "pos_cashier_items_cache")
data class SajilZatcaEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val priceSar: Double,
    val badge: String,
    val category: String,
    val status: String
) {
    fun toDomain(): SajilZatcaItem = SajilZatcaItem(
        id = id,
        title = title,
        description = description,
        priceSar = priceSar,
        badge = badge,
        category = category,
        status = status
    )
}
